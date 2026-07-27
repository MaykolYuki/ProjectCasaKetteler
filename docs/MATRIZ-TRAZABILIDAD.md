# Matriz de Trazabilidad

Relaciona cada **requisito** de la Especificación con el **componente** del sistema que
lo implementa y con la **prueba** que lo verifica. La numeración de requisitos sigue la
versión actual de `ESPECIFICACIÓN DE REQUISITOS.docx`.

**Leyenda de verificación:**
- `ESC-U-##` / `PU-##` → escenario y casos de prueba **unitaria** (ver puntos 7.5, 7.6 y 8.1).
- `Carga` / `Estrés` → verificado en las pruebas de rendimiento (8.2 y 8.3).
- `Funcional` → verificado mediante prueba funcional/manual de la aplicación.
- `Operativa` → objetivo operativo, se comprueba en el despliegue/operación.

**Estado:** ✅ Implementado · ◑ Parcial · ○ Pendiente / trabajo futuro.

---

## Requerimientos Funcionales

| Req. | Descripción (resumen) | Componente / Módulo | Verificación | Estado |
|------|------------------------|---------------------|--------------|:------:|
| RF-01 | Autenticación con contraseña cifrada (BCrypt) | `BusinessUser.login` | ESC-U-02 · PU-08 | ✅ |
| RF-02 | Dos roles: Administrador y Residente | `SecurityConfig`, entidades | Funcional | ✅ |
| RF-03 | Verificación de rol en cada petición (RBAC) | `SecurityConfig`, `JwtRequestFilter` | Funcional | ✅ |
| RF-04 | Módulo de gestión de residentes (crear/editar/desactivar/eliminar/reset) | `UserController`, `BusinessUser` | Funcional · PU-13 (desactivada) | ✅ |
| RF-05 | Contraseña temporal al registrar | `BusinessUser.insert` | Funcional | ✅ |
| RF-06 | Pantalla de perfil del residente | `myprofile`, frontend perfil | Funcional | ✅ |
| RF-07 | Sesión única por usuario | `BusinessUser.login` (`tokenValidAfter`), `JwtRequestFilter` | ESC-U-02 · PU-09 / ESC-U-07 · PU-30, PU-31 | ✅ |
| RF-08 | Persistencia de sesión distinta por rol | Frontend · utilidad de almacenamiento | ESC-U-07 · PU-27, PU-28 | ✅ |
| RF-09 | Acceso del admin por navegador servido por el backend | `config` interfaz web estática | Funcional | ✅ |
| RF-10 | Activar cámara frontal para capturar rostro | Frontend/Capacitor (app Android) | Funcional | ✅ |
| RF-11 | Ráfaga de imágenes + mediana de coincidencia | Servicio Python + `BusinessAttendance` | ESC-U-01 · PU-06 (rama de rechazo) | ✅ |
| RF-12 | Cada marca es un evento con fecha, tipo, motivo, red, IP, coincidencia | `BusinessAttendance`, `EntityAttendance` | ESC-U-01 · PU-01, PU-02 | ✅ |
| RF-13 | Motivo de salida obligatorio (lista + "Otros"); entrada opcional | Frontend residente + `BusinessAttendance` | ESC-U-08 · PU-34…PU-38 | ✅ |
| RF-14 | Rechazo de marcas duplicadas < 5 minutos | `BusinessAttendance` | ESC-U-01 · PU-03 | ✅ |
| RF-15 | Notificación visual de resultado (color éxito/error) | Frontend residente | Funcional | ✅ |
| RF-16 | Umbral: mediana ≥ 55 % | `BusinessAttendance` | ESC-U-01 · PU-06 | ✅ |
| RF-17 | Marcar anomalía (dos eventos del mismo tipo) | `BusinessAttendance` | ESC-U-01 · PU-04, PU-05 | ✅ |
| RF-18 | Verificar red WiFi antes de habilitar la marca | `BusinessAttendance`, app | ESC-U-01 · PU-07 | ✅ |
| RF-19 | Validar por BSSID (o SSID en su defecto) | `BusinessAttendance` (residencia) | ESC-U-01 · PU-07 | ✅ |
| RF-20 | No bloquear por falta de Internet si el SSID local es válido | `BusinessAttendance` | Funcional | ✅ |
| RF-21 | Mensaje de red válida/ inválida | Frontend residente | Funcional | ✅ |
| RF-22 | Cargar y asociar documentos a un residente | `DocumentGeneralController` | Funcional | ✅ |
| RF-23 | Clasificar documentos por categoría | `BusinessDocumentGeneral` | Funcional | ✅ |
| RF-24 | Listar documentos por residente (ver/descargar) | `DocumentGeneralController` | Funcional | ✅ |
| RF-25 | Formulario mensual de constancia de pago | Frontend + documentos | Funcional | ✅ |
| RF-26 | Validar formato/tamaño (PDF/JPG/PNG ≤ 10 MB) | Validación de carga | Funcional | ✅ |
| RF-27 | Formulario semestral de constancia de notas | Frontend + documentos | Funcional | ✅ |
| RF-28 | Listado cronológico de documentos | `BusinessDocumentGeneral` | Funcional | ✅ |
| RF-29 | Mostrar al residente solo lo marcado como descargable | `DocumentGeneralController` | Funcional | ✅ |
| RF-30 | Admin carga archivo de renuncia y lo asocia | `DocumentResignationController` | Funcional | ✅ |
| RF-31 | Residente descarga el formato de renuncia asignado | `DocumentResignationController` | Funcional | ✅ |
| RF-32 | Residente sube la renuncia firmada (queda pendiente) | `DocumentResignationController` | Funcional | ✅ |
| RF-33 | Estado del documento (Pendiente/Aprobado/Observado/Rechazado) | Enum de estado + business docs | Funcional | ✅ |
| RF-34 | Observaciones del admin visibles al residente | `BusinessDocument*` | Funcional | ✅ |
| RF-35 | Respaldos automáticos (BD diario, archivos semanal, 14 días, manual) | `BackupService`, `BackupController` | ESC-U-04 · PU-19…PU-21 | ✅ |
| RF-36 | Filtros de asistencia (fecha, rango, diario, semana, mes, residente, estado) | `AttendanceController` + frontend reportes | ESC-U-06 · PU-23…PU-26 | ✅ |
| RF-37 | Tabla de horas de ingreso/salida por residente | `AttendanceController` | Funcional | ✅ |
| RF-38 | Exportar reportes en Excel (.xlsx) y PDF | `BusinessAttendanceExport`, `attendance/export` | Funcional | ✅ |
| RF-39 | Reconocimiento en servicio dedicado en el servidor (sin terceros) | Servicio Python local (Flask) | Funcional | ✅ |
| RF-40 | Operación en red local; sin Internet, misma red que el servidor | Arquitectura de red | Funcional / Operativa | ✅ |

## Reglas de Negocio

| Req. | Descripción (resumen) | Componente / Módulo | Verificación | Estado |
|------|------------------------|---------------------|--------------|:------:|
| RN-01 | Marca solo desde la red WiFi oficial (BSSID/SSID) | `BusinessAttendance` | ESC-U-01 · PU-07 | ✅ |
| RN-02 | La asistencia corresponde al residente real (rostro) | `BusinessAttendance` + servicio facial | ESC-U-01 · PU-06 | ✅ |
| ~~RN-03~~ | ~~Sincronización diferida offline~~ | — | — | ○ *Eliminar (ver correcciones)* |

## Requerimientos No Funcionales

| Req. | Descripción (resumen) | Componente / Módulo | Verificación | Estado |
|------|------------------------|---------------------|--------------|:------:|
| RNF-01 | RBAC + cifrado de datos | `SecurityConfig` | Funcional | ✅ |
| RNF-02 | BCrypt (y HTTPS en producción) | `PasswordEncoder` | ESC-U-02 · PU-08 | ✅ (HTTPS: pendiente en prod) |
| RNF-03 | Bloqueo tras 5 intentos fallidos | `BusinessUser.login` | ESC-U-02 · PU-10, PU-11, PU-12 | ✅ |
| RNF-04 | Filtrado de documentos por usuario autenticado | `BusinessDocument*` | Funcional | ✅ |
| RNF-05 | Reconocimiento facial en 3–5 s en CPU | Servicio Python | Medición empírica | ✅ |
| RNF-06 | Respuesta administrativa < 3 s (≤ 500 registros/día) | Backend + BD | Carga (8.2) | ✅ |
| RNF-07 | Estabilidad con hasta 200 residentes | Backend + BD | Carga / Estrés (8.2, 8.3) | ✅ |
| RNF-08 | Guía de estilos unificada | Frontend (Angular) | Funcional | ✅ |
| RNF-09 | Flujo de marca en máx. 3 pasos | Frontend residente | Funcional | ✅ |
| RNF-10 | Mensajes descriptivos diferenciados | Frontend + backend | Funcional · PU-32, PU-33 | ✅ |
| RNF-11 | Diseño responsive desde 720×1280 | Frontend | Funcional | ✅ |
| RNF-12 | Nombres de archivo legibles `TIPO_Nombre_Apellido_fecha` | `DocumentNameHelper` | ESC-U-03 · PU-14…PU-18 | ✅ |
| RNF-13 | Disponibilidad ≥ 95 % mensual | Operación del servidor | Operativa | ✅ (objetivo) |
| ~~RNF-14~~ | ~~Almacenar offline y sincronizar~~ | — | — | ○ *Eliminar (ver correcciones)* |
| RNF-15 | Arranque automático al encender el servidor | Task Scheduler + scripts | ESC-U-05 · PU-22 (arranque) / Operativa | ✅ |
| RNF-16 | Compatible con Android 10 (API 29) o superior | App (Capacitor) | Funcional | ✅ |
| RNF-17 | Layouts flexibles (dp/sp) | Frontend | Funcional | ✅ |
| RNF-18 | Arquitectura modular por capas | Estructura del proyecto | Revisión de código | ✅ |
| RNF-19 | Código comentado y con convenciones | Código fuente | Revisión de código | ✅ |
| RNF-20 | Extensible sin modificar lo existente | Arquitectura | Revisión de código | ✅ |
| RNF-21 | Tasa de reconocimiento correcto ≥ 90 % | Servicio facial | Medición empírica | ✅ |
| RNF-22 | Liveness / anti-spoofing + umbral sobre la mediana | Servicio facial | Medición empírica | ✅ |
| RNF-23 | Restricción de edición de asistencia (permisos BD auditados) | Backend (RBAC) | Funcional | ◑ *Parcial: RBAC sí; permisos BD/auditoría no (ver correcciones)* |
| RNF-24 | Aviso al usuario cuando el servidor no responde | Frontend (interceptor) | Funcional | ✅ |

---

## Cobertura por tipo de prueba (resumen)

| Tipo de prueba | Requisitos que verifica |
|----------------|--------------------------|
| **Unitaria** (8.1) | RF-01, RF-03(parc.), RF-07, RF-08, RF-11, RF-12, RF-13, RF-14, RF-16, RF-17, RF-18, RF-19, RF-35, RF-36, RN-01, RN-02, RNF-02, RNF-03, RNF-10, RNF-12, RNF-15 |
| **Carga** (8.2) | RNF-06, RNF-07 |
| **Estrés** (8.3) | RNF-07 |
| **Medición empírica** | RNF-05, RNF-21, RNF-22 |
| **Funcional / manual** | El resto de RF y RNF de interfaz y gestión documental |

> **Nota:** los requisitos de reconocimiento facial (RF-11, RN-02, RNF-05, RNF-21,
> RNF-22) corren en el servicio Python; las pruebas unitarias del backend verifican la
> *integración* con ese servicio (por ejemplo, el rechazo cuando el rostro no coincide,
> PU-06), mientras que la precisión del modelo se comprueba por medición empírica.
