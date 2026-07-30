# Plan de despliegue en una plataforma en la nube

Evaluación de viabilidad y hoja de ruta para publicar **Casa Ketteler** en una
plataforma como Render, en lugar de operarlo únicamente en la computadora de la
residencia.

---

## 1. Conclusión primero

**Un despliegue completo en el plan gratuito de Render no es viable.** El obstáculo no
es el backend ni la base de datos: es el **servicio de reconocimiento facial**.

| Recurso | Lo que necesita | Lo que ofrece el plan gratuito |
|---------|-----------------|-------------------------------|
| Memoria del servicio de reconocimiento | > 1 GB (TensorFlow + modelos) | **512 MB** |
| Almacenamiento de fotos y documentos | Persistente | **No hay disco persistente** |
| Base de datos | MySQL | Solo PostgreSQL, y **caduca a los 30 días** |
| Disponibilidad | Inmediata al marcar asistencia | **Se apaga a los 15 min** de inactividad; despertar tarda ~1 min |

Ese último punto merece atención aparte: aunque todo lo demás se resolviera, un
residente que marca asistencia tendría que esperar cerca de un minuto a que el servicio
despierte. Para un sistema cuyo propósito es registrar entradas y salidas con agilidad,
eso lo vuelve inservible en la práctica.

**Recomendación:** exponer el sistema actual mediante un **túnel** (opción C). Da una
dirección pública con HTTPS, sin coste, sin reescribir nada y sin perder el
reconocimiento facial.

---

## 2. Lo que impide mover el sistema tal como está

Además de los límites de la plataforma, el código tiene tres acoplamientos que hoy
asumen que **todo corre en la misma máquina**:

| # | Acoplamiento | Dónde | Consecuencia |
|---|--------------|-------|--------------|
| 1 | **Sistema de archivos compartido** | El backend le pasa a Python **rutas de archivo** (`directorio`, `rutaReferencia`), y Python las abre desde su propio `STORAGE_PATH` | Si backend y reconocimiento son dos servicios separados, Python no encuentra las fotos |
| 2 | **URL del reconocimiento fija en el código** | `PythonFaceRecognitionHelper`: `SERVER_URL = "http://localhost:5000"` (constante) | No se puede apuntar a otro servidor sin recompilar |
| 3 | **Fotos y documentos en disco local** | 5 clases de negocio escriben en `app.storage.path` | En la nube el disco es efímero: al reiniciar se pierden |

**Lo que sí está preparado:** la base de datos usa **0 consultas SQL nativas** (las 16
`@Query` son JPQL), así que migrar de MySQL a PostgreSQL es viable sin reescribir
lógica. Y la validación por Wi-Fi **no se rompe**: la app lee el SSID/BSSID en el
propio celular y los envía, así que funciona aunque el servidor esté lejos.

---

## 3. Opciones

### Opción A — Todo en la nube (Render de pago)

| Componente | Instancia | Coste aprox. |
|------------|-----------|-------------:|
| Backend + interfaz web | Starter (512 MB) | 7 USD/mes |
| Reconocimiento facial | Standard (2 GB) | 25 USD/mes |
| Base de datos | PostgreSQL de pago | ~7 USD/mes |
| Almacenamiento de archivos | Disco persistente u objeto | ~2 USD/mes |
| | **Total** | **~40 USD/mes** |

Requiere además resolver los tres acoplamientos del apartado 2 y migrar a PostgreSQL.

> **Veredicto:** técnicamente correcto, económicamente inviable para un proyecto
> universitario, y con semanas de trabajo por delante.

### Opción B — Despliegue parcial, solo para demostración

Subir a Render **backend + interfaz web + PostgreSQL** (gratis), **sin reconocimiento
facial**. Permite mostrar el sistema funcionando desde cualquier lugar: iniciar sesión,
gestión de usuarios, documentos, reportes.

- **Coste:** 0
- **Esfuerzo:** medio (migrar a PostgreSQL, almacenamiento de archivos, variables)
- **Limitación:** **no se puede marcar asistencia**, que es el corazón del sistema
- **Caducidad:** la base de datos gratuita expira a los 30 días

> **Veredicto:** sirve si el objetivo es *enseñar* el sistema. No sirve para operarlo.

### Opción C — Túnel al servidor de la residencia ⭐ recomendada

El sistema sigue donde está (la computadora de la residencia) y se publica hacia fuera
con **Cloudflare Tunnel**: un pequeño programa abre una conexión **saliente** hacia
Cloudflare, que a cambio entrega una dirección pública con HTTPS. No hace falta IP
fija, ni abrir puertos en el router.

- **Coste:** 0
- **Esfuerzo:** bajo (instalar un programa y crear el túnel)
- **Reconocimiento facial:** sigue funcionando, corre en la misma máquina
- **Resuelve el problema de la IP:** la app apunta a un dominio estable, no a `192.168.x.x`
- **Añade HTTPS**, que hoy es una limitación conocida del sistema

**Contrapartidas honestas:** si la computadora de la residencia se apaga, el sistema
deja de estar disponible (igual que hoy); y el tráfico pasa por un tercero, aunque
cifrado.

---

## 4. Comparación

| | A: Todo en la nube | B: Demostración | C: Túnel |
|---|:---:|:---:|:---:|
| Coste mensual | ~40 USD | 0 | 0 |
| Se puede marcar asistencia | Sí | **No** | Sí |
| Esfuerzo de desarrollo | Alto | Medio | **Bajo** |
| Resuelve el problema de la IP | Sí | Sí | Sí |
| Añade HTTPS | Sí | Sí | Sí |
| Depende del equipo de la residencia | No | No | Sí |
| Caduca | No | **A los 30 días** | No |

---

## 5. Pasos para la opción recomendada (C)

1. **Instalar `cloudflared`** en la computadora de la residencia.
2. **Crear el túnel** apuntando a `http://localhost:8001`.
3. **Anotar la dirección pública** que entrega Cloudflare.
4. **Actualizar el `.env`**: añadir esa dirección a `CORS_ALLOWED_ORIGINS`.
5. **Regenerar el APK** apuntando a la nueva dirección (ver apartado 6).
6. **Publicar el APK** en la página de descarga.
7. **Dejar el túnel como servicio** para que arranque con la computadora, igual que ya
   hace el sistema.

> La interfaz web del administrador **no necesita cambios**: ya usa una URL relativa,
> así que funciona con cualquier dominio automáticamente.

---

## 6. Cambio necesario en la app móvil

La app tiene la dirección del servidor fijada al compilar, en
`src/environments/environment.prod.ts`:

```ts
apiBaseUrl: 'http://192.168.101.3:8001/casaketteler',
```

Al tener una dirección pública, se reemplaza por ella (con `https://`) y se regenera:

```bash
# en el proyecto del frontend
npm run build:android
cp dist/front-ketteler/browser/index.csr.html dist/front-ketteler/browser/index.html
npx cap sync android
cd android && ./gradlew.bat assembleRelease
```

> **Importante:** subir `versionCode` en `android/app/build.gradle` en cada entrega, o
> Android no instalará la nueva app sobre la anterior.

### Mejora recomendada a futuro

Cada cambio de dirección obliga hoy a recompilar y repartir el APK de nuevo. Convendría
que la dirección **se pueda configurar dentro de la app** (una pantalla de ajustes, o
leerla de un archivo remoto al arrancar). Con eso, un cambio de servidor dejaría de
requerir una nueva versión para todos los residentes.

---

## 7. Si se opta por la nube de todos modos (A o B)

Trabajo previo imprescindible, en orden:

| # | Tarea | Motivo |
|---|-------|--------|
| 1 | Hacer configurable la URL del reconocimiento (`SERVER_URL` → variable de entorno) | Hoy está fija en el código |
| 2 | Enviar la foto de referencia **como base64** en vez de como ruta | Elimina la dependencia del disco compartido; el envío de la ráfaga ya usa base64 |
| 3 | Mover fotos y documentos a almacenamiento de objetos (S3, R2) | El disco de la nube es efímero |
| 4 | Migrar de MySQL a PostgreSQL | No hay MySQL gratuito; sin consultas nativas, es viable |
| 5 | Sustituir los respaldos con `mysqldump` | La herramienta cambia con el motor |
| 6 | Revisar el arranque automático y las tareas programadas | En la nube no hay Programador de tareas de Windows |

---

## 8. Fuentes

Los límites citados se verificaron el 30 de julio de 2026:

- [Render — Free tier](https://render.com/docs/free): las instancias gratuitas se
  apagan tras 15 minutos sin tráfico y tardan alrededor de un minuto en volver; no
  admiten disco persistente; PostgreSQL gratuito con 1 GB y caducidad a 30 días.
- [Render — precios e instancias](https://www.srvrlss.io/provider/render/): Free con
  512 MB y 0.1 CPU; Starter 512 MB por 7 USD; Standard 2 GB por 25 USD.
- [Cloudflare — Tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/):
  conecta un servidor sin dirección IP pública mediante conexiones salientes, sin abrir
  puertos.

> Conviene confirmar precios y límites antes de contratar: cambian con frecuencia.
