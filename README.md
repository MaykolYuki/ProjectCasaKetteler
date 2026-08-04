# Casa Ketteler — Sistema de control de asistencia

Sistema para una residencia universitaria que registra la entrada y salida de los
residentes mediante **reconocimiento facial**, validando además que la marca se haga
**desde la red Wi-Fi de la residencia**.

- Los **residentes** marcan asistencia desde una app Android.
- La **administradora** gestiona residentes, documentos y reportes desde el navegador.

---

## Índice de documentación

| Documento | Para quién | Qué responde |
|-----------|-----------|--------------|
| [docs/INSTALACION.md](docs/INSTALACION.md) | Desarrollador | Cómo montar el proyecto en otra computadora |
| [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md) | Quien instala | Cómo llevarlo a la PC de la residencia |
| [docs/OPERACION.md](docs/OPERACION.md) | Quien lo usa a diario | Encender, apagar, respaldos y problemas comunes |
| [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md) | Desarrollador / evaluador | Cómo está construido y por qué |

---

## Las piezas del sistema

El sistema son **tres programas** que trabajan juntos:

```
   ┌──────────────────┐        ┌──────────────────┐
   │  App Android     │        │   Navegador      │
   │  (residentes)    │        │ (administradora) │
   └────────┬─────────┘        └────────┬─────────┘
            │  marca asistencia          │  gestiona
            │                            │
            └────────────┬───────────────┘
                         ▼
            ┌────────────────────────────┐
            │   Backend  ·  Java :8001   │  ← también entrega la web
            └───────┬────────────┬───────┘
                    │            │
         ¿es esta   │            │  guarda
         persona?   ▼            ▼
      ┌──────────────────┐   ┌──────────┐
      │ Reconocimiento   │   │  MySQL   │
      │ facial · Py :5000│   └──────────┘
      └──────────────────┘
```

| Componente | Tecnología | Puerto |
|------------|-----------|--------|
| Backend + interfaz web | Java 21 · Spring Boot 4 | **8001** |
| Reconocimiento facial | Python · DeepFace · Flask | **5000** |
| Base de datos | MySQL 8 | 3306 |
| App móvil | Angular 21 + Capacitor | — |

> El backend **también sirve la interfaz web**, así que no hace falta instalar Node
> en la computadora de la residencia.

---

## Inicio rápido

**¿Solo quieres encender el sistema?** → doble clic en `iniciar-casa-ketteler.bat`
y abre `http://localhost:8001`. Ver [OPERACION](docs/OPERACION.md).

**¿Vas a programar?** → ver [INSTALACION](docs/INSTALACION.md).

**¿Vas a instalarlo en la residencia?** → ver [DESPLIEGUE](docs/DESPLIEGUE.md).

---

## Comandos más usados

```powershell
# --- Backend ---
.\mvnw.cmd clean package     # genera el JAR (detén el sistema antes)
.\mvnw.cmd test              # ejecuta las pruebas
.\ejecutar-backend.ps1       # ejecuta solo el backend

# --- Sistema completo ---
.\iniciar-casa-ketteler.bat  # enciende backend + reconocimiento facial
.\detener-casa-ketteler.bat  # apaga ambos
.\actualizar-frontend.ps1    # publica una nueva versión de la interfaz web
```

```powershell
# --- Frontend (en la carpeta del proyecto Angular) ---
npx ng build                    # compila la interfaz
npx ng test --watch=false       # ejecuta las pruebas
npm run ship:android            # compila e instala la app en un celular conectado
```

---

## Estado de las pruebas

| Proyecto | Comando | Pruebas |
|----------|---------|---------|
| Backend | `.\mvnw.cmd test` | 22 |
| Frontend | `npx ng test --watch=false` | 16 |

Cubren las reglas críticas: registro de asistencia (alternancia entrada/salida,
espera entre marcas, anomalías), seguridad del login (bloqueo por intentos, sesión
única) y los respaldos.

---

## Funcionalidades principales

**Residentes (app Android)**
- Marcar entrada/salida con reconocimiento facial y validación de red Wi-Fi
- Consultar su historial y el registro del día
- Subir y descargar sus documentos, y la carta de renuncia
- Ver su perfil con su foto

**Administración (navegador)**
- Panel con indicadores del día (residentes, presentes, ausentes) y actividad reciente
- Gestión de residentes: crear, editar, desactivar, restablecer contraseña
- Revisión de documentos: aprobar, rechazar y observar
- Reportes de asistencia con filtros y exportación a Excel/PDF
- Respaldo del sistema bajo demanda
