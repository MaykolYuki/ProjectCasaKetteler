# Arquitectura y decisiones técnicas

Cómo está construido el sistema y **por qué** se tomó cada decisión.

---

## Visión general

```
        App Android (residente)          Navegador (administración)
                 │                                  │
                 └───────────────┬──────────────────┘
                                 ▼
                   ┌─────────────────────────────┐
                   │   Backend · Spring Boot     │
                   │   :8001                     │
                   │  ┌───────────────────────┐  │
                   │  │ Controllers (REST)    │  │
                   │  ├───────────────────────┤  │
                   │  │ Business (reglas)     │  │
                   │  ├───────────────────────┤  │
                   │  │ Repositories (JPA)    │  │
                   │  └───────────────────────┘  │
                   └────┬───────────────────┬────┘
                        │                   │
                        ▼                   ▼
          ┌──────────────────────┐    ┌──────────┐
          │ Reconocimiento facial│    │  MySQL   │
          │ Flask + DeepFace     │    └──────────┘
          │ :5000                │
          └──────────────────────┘
```

**Capas del backend:** los *controllers* solo reciben y responden; las *business*
concentran las reglas; los *repositories* hablan con la base de datos.

---

## Por qué el reconocimiento facial va aparte

Las librerías de visión por computadora (DeepFace, TensorFlow) son de Python y no
tienen equivalente maduro en Java. En vez de forzarlo, se ejecuta como **un servicio
independiente** al que el backend consulta por HTTP.

**Ventaja:** si el reconocimiento falla o se reinicia, el resto del sistema sigue
funcionando (consultas, documentos, reportes).

---

## Cómo se decide si un rostro es válido

Este fue el punto más delicado del proyecto.

### El problema

La primera versión comparaba **una sola foto** y confiaba en el veredicto interno del
modelo. Con datos reales resultó **demasiado permisivo**: un impostor con 34% de
parecido era aceptado.

### La solución

1. **Ráfaga de 3 fotos** en lugar de una.
2. Se calcula la **mediana** de las similitudes → una foto mala (parpadeo, movimiento)
   no arrastra el resultado.
3. Se aplica un **umbral propio del 55%**, ignorando el veredicto del modelo.

### Resultado medido

| Caso | Similitud | Decisión |
|------|-----------|----------|
| Residente correcto | 78% | ✅ Aceptado |
| Impostor | 0% | ❌ Rechazado |

El margen entre ambos es amplio, así que el umbral es robusto.

### Además: velocidad

Se cambió el detector de rostros de `retinaface` a **`ssd`**:

| | Antes | Después |
|--|-------|---------|
| Tiempo por marca | ~95 s | **~3–5 s** |

Con `retinaface`, 40 residentes marcando habrían hecho una cola inviable. Se verificó
que `ssd` mantiene la misma capacidad de distinguir personas.

Los modelos se **precargan al arrancar** para que la primera marca no pague ese costo.

---

## Doble validación al marcar asistencia

Para registrar una entrada o salida deben cumplirse **dos condiciones**:

1. **Identidad** — el rostro coincide (≥55%).
2. **Ubicación** — el celular está en la Wi-Fi de la residencia (se compara el BSSID
   del punto de acceso, o el nombre de red como alternativa).

Esto evita que alguien marque desde fuera aunque su rostro sea correcto.

Si falla cualquiera de las dos, se guarda un **INTENTO_FALLIDO** con el motivo, para
que la administración pueda revisarlo.

---

## Modelo de eventos

La asistencia **no** guarda "una fila por día con hora de entrada y salida", sino
**un evento por cada marca**:

```
ENTRADA         2026-07-23 07:15
SALIDA          2026-07-23 08:02
INTENTO_FALLIDO 2026-07-23 08:05   (rostro no reconocido)
ENTRADA         2026-07-23 13:40
```

**Por qué:** un residente puede entrar y salir varias veces al día. Además permite
auditar intentos fallidos y detectar irregularidades.

**Anomalías:** si llegan dos eventos seguidos del mismo tipo (dos entradas sin salida
intermedia), el evento se marca como anomalía para revisión.

**Estado actual:** cada residente tiene un indicador `presente` que determina si su
próxima marca será entrada o salida.

---

## Seguridad

### Autenticación
Tokens **JWT** sin estado. Cada petición viaja con su token; el backend lo valida y
extrae el rol.

### Sesión única
Al iniciar sesión se registra el momento (`tokenValidAfter`) y **se invalidan los
tokens anteriores**. Iniciar sesión en otro dispositivo cierra el anterior.

> Detalle fino: el `iat` del JWT está en segundos y `tokenValidAfter` en milisegundos.
> Se resta un margen de 5 s, o el token recién emitido se rechazaría a sí mismo.

### Persistencia distinta según el rol

| Rol | Dónde se guarda | Efecto |
|-----|----------------|--------|
| Residente | `localStorage` | La sesión sobrevive; no vuelve a iniciar sesión |
| Administración | `sessionStorage` | Muere al cerrar el navegador |

**Por qué:** el celular es personal, la computadora de la residencia es compartida.

### Otras medidas
- Contraseñas con **BCrypt** (nunca en texto plano)
- **Bloqueo temporal** tras 5 intentos fallidos
- Permisos por rol (`RESIDENTE`, `ADMIN`, `SUPER_ADMIN`)
- Cada residente solo accede a **sus** documentos

---

## La interfaz web la sirve el backend

En lugar de un servidor web aparte, el backend entrega los archivos de la interfaz
desde una carpeta externa (`frontend/`).

**Por qué:**
- No hay que instalar Node en la computadora de la residencia
- Un proceso menos que administrar
- Al ser el mismo origen, no hay problemas de CORS

Como la carpeta es externa, **se puede actualizar la interfaz sin regenerar el JAR**.

---

## Una base de código, dos presentaciones

El mismo proyecto Angular produce:
- La **web** para administración (servida por el backend)
- La **app Android**, empaquetada con **Capacitor**

Capacitor permite acceder a la cámara y a la información de Wi-Fi, imprescindibles
para el control de asistencia.

---

## Respaldos

| Qué | Cuándo | Formato |
|-----|--------|---------|
| Base de datos | Diario 2:00 a. m. | `.sql` (mysqldump) |
| Fotos y documentos | Domingos 2:30 a. m. | `.zip` |

Se conservan 14 días. Se usa `--single-transaction` para **no bloquear la base**
mientras se respalda, y la contraseña viaja por variable de entorno para que no
quede visible en la lista de procesos.

---

## Pruebas

| Proyecto | Cantidad | Enfoque |
|----------|----------|---------|
| Backend | 22 | Reglas de negocio con Mockito (sin base de datos) |
| Frontend | 16 | Lógica pura y reglas de sesión |

Se priorizó **cubrir las reglas críticas** antes que alcanzar un porcentaje:
asistencia (alternancia, espera entre marcas, anomalías), seguridad del login
(bloqueo, sesión única) y respaldos.

Al no depender de la base de datos, corren en segundos y en cualquier computadora.

---

## Limitaciones conocidas

| Limitación | Impacto | Camino de solución |
|------------|---------|--------------------|
| Sin HTTPS | El tráfico viaja sin cifrar en la red local | Certificado SSL al publicar fuera |
| IP fija en la app | Si cambia la IP del servidor hay que regenerar el APK | Nombre de dominio o IP reservada |
| Respaldos en el mismo disco | Una avería del disco los perdería | Copia periódica a USB o nube |
| Solo funciona en la red local | Es intencional: la asistencia debe ser presencial | — |
| Un servidor Flask de desarrollo | Suficiente para ~40 residentes | Servidor WSGI si creciera |

---

## Estructura del código

```
ProjectCasaKetteler/
├── src/main/java/com/epiis/projectcasaketteler/
│   ├── controller/   → endpoints REST
│   ├── business/     → reglas de negocio
│   ├── repository/   → acceso a datos (JPA)
│   ├── entity/       → tablas
│   ├── dto/          → objetos de entrada y salida
│   ├── config/       → seguridad, CORS, interfaz web
│   ├── helper/       → utilidades (JWT, nombres, correo)
│   └── service/      → servicios (respaldo, usuarios)
├── python_scripts/   → reconocimiento facial
├── storage/          → fotos y documentos
├── frontend/         → interfaz web compilada
└── docs/             → esta documentación
```

```
front-ketteler/
└── src/app/
    ├── admin/        → pantallas de administración
    ├── residente/    → pantallas del residente
    ├── auth/         → inicio de sesión
    └── core/         → servicios, guards, interceptores
```
