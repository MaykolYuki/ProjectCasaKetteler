# Casa Ketteler — Sistema de control de asistencia

Sistema para una residencia universitaria que registra la entrada y salida de los
residentes mediante **reconocimiento facial**, validando además que la marca se haga
**desde la red Wi-Fi de la residencia**.

- Los **residentes** marcan asistencia desde una app Android.
- La **administradora** gestiona residentes, documentos y reportes desde el navegador.

---

## Índice de documentación

**Para trabajar con el sistema**

| Documento | Para quién | Qué responde |
|-----------|-----------|--------------|
| [docs/INSTALACION.md](docs/INSTALACION.md) | Desarrollador | Cómo montar el proyecto en otra computadora |
| [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md) | Quien instala | Cómo llevarlo a la PC de la residencia |
| [docs/OPERACION.md](docs/OPERACION.md) | Quien lo usa a diario | Encender, apagar, respaldos y problemas comunes |
| [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md) | Desarrollador / evaluador | Cómo está construido y por qué |
| [docs/MODELO-BD.md](docs/MODELO-BD.md) | Desarrollador / evaluador | Modelo lógico y físico de la base de datos |

**Documentación del informe**

| Documento | Sección | Contenido |
|-----------|---------|-----------|
| [docs/REQUISITOS-ACTUALIZADOS.md](docs/REQUISITOS-ACTUALIZADOS.md) | — | Requisitos revisados frente a lo implementado |
| [docs/VALIDACION-REQUISITOS.md](docs/VALIDACION-REQUISITOS.md) | — | Validación requisito por requisito |
| [docs/CORRECCIONES-ESPECIFICACION.md](docs/CORRECCIONES-ESPECIFICACION.md) | — | Correcciones propuestas a la especificación |
| [docs/DISENO-ESCENARIOS-UNITARIAS.md](docs/DISENO-ESCENARIOS-UNITARIAS.md) | 7.5 | Diseño de escenarios de prueba unitaria |
| [docs/MATRIZ-TRAZABILIDAD.md](docs/MATRIZ-TRAZABILIDAD.md) | 7.7 | Trazabilidad requisito ↔ componente ↔ prueba |
| [docs/PRUEBAS.md](docs/PRUEBAS.md) | 8 | Pruebas unitarias, de carga y de estrés |
| [docs/GESTION-RIESGOS.md](docs/GESTION-RIESGOS.md) | 5.4 | Identificación, análisis y mitigación de riesgos |
| [docs/MATRIZ-ACCESIBILIDAD.md](docs/MATRIZ-ACCESIBILIDAD.md) | — | Evaluación de accesibilidad (axe DevTools, WCAG 2.1) |
| [docs/ACCESIBILIDAD-RESULTADOS.md](docs/ACCESIBILIDAD-RESULTADOS.md) | — | Resultados tras aplicar las correcciones |
| [docs/PLAN-DESPLIEGUE-NUBE.md](docs/PLAN-DESPLIEGUE-NUBE.md) | — | Análisis de despliegue en la nube |

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

# --- Instalación y actualización en la residencia ---
.\construir-instalador.ps1   # arma el instalador .exe (un solo archivo)
.\actualizar-sistema.ps1     # actualiza una instalación ya existente
.\preparar-entrega.ps1       # arma el paquete de entrega completo
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
| Frontend | `npx ng test --watch=false` | 20 |

Cubren las reglas críticas: registro de asistencia (alternancia entrada/salida,
espera entre marcas, anomalías), seguridad del login (bloqueo por intentos, sesión
única), el aislamiento del perfil entre sesiones y los respaldos.

> En el frontend hay que usar **`ng test`**: el constructor de Angular prepara antes
> el entorno de pruebas, y al invocar `vitest` directamente las pruebas fallan aunque
> el código esté bien. El detalle está en [docs/PRUEBAS.md](docs/PRUEBAS.md).

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
