# 7.5 Diseño de Escenarios — Pruebas Unitarias

Un **escenario de prueba** describe, a alto nivel, una situación o funcionalidad que se
somete a verificación. Cada escenario agrupa varios **casos de prueba** concretos
(detallados en el punto 7.6) y se vincula con los **requisitos** que valida (matriz de
trazabilidad, punto 7.7).

Las pruebas unitarias del sistema se organizan en **9 escenarios** (5 de backend y 4 de
frontend) que reúnen los 42 casos de prueba (PU-01 a PU-42).

> Los identificadores de requisito (RF/RN/RNF) siguen la numeración de la Especificación
> de Requisitos actualizada.

---

## Resumen de escenarios

| ID | Escenario | Componente | Casos | Requisitos |
|----|-----------|------------|:-----:|------------|
| ESC-U-01 | Registro de asistencia del residente | Backend · `BusinessAttendance` | PU-01 … PU-07 | RF-11, RF-12, RF-14, RF-16, RF-17, RF-18, RF-19, RN-01, RN-02 |
| ESC-U-02 | Autenticación y seguridad de acceso | Backend · `BusinessUser` (login) | PU-08 … PU-13 | RF-01, RNF-02, RNF-03 |
| ESC-U-03 | Generación de nombres de documentos | Backend · `DocumentNameHelper` | PU-14 … PU-18 | RNF-12 |
| ESC-U-04 | Respaldo automático de archivos | Backend · `BackupService` | PU-19 … PU-21 | RF-35 |
| ESC-U-05 | Arranque del sistema | Backend · Contexto Spring | PU-22 | RNF-15 |
| ESC-U-06 | Cálculo de la fecha local en reportes | Frontend · Utilidad de fecha | PU-23 … PU-26 | RF-36 |
| ESC-U-07 | Almacenamiento de la sesión según el rol | Frontend · Utilidad de almacenamiento | PU-27 … PU-33 | RF-07, RF-08 |
| ESC-U-08 | Registro del motivo de asistencia | Frontend · Vista del residente | PU-34 … PU-38 | RF-13 |
| ESC-U-09 | Aislamiento del perfil entre sesiones | Frontend · Servicio de perfil | PU-39 … PU-42 | RF-06, RF-07, RF-08 |

---

## ESC-U-01 · Registro de asistencia del residente

| Campo | Descripción |
|-------|-------------|
| **Objetivo** | Verificar que la lógica de marcado aplica correctamente las reglas de negocio: alternancia entrada/salida según la presencia, ventana mínima entre marcas, detección de anomalías, verificación facial y validación de red. |
| **Componente bajo prueba** | `BusinessAttendance` (capa de negocio del backend). |
| **Precondiciones** | Dependencias simuladas con *mocks* (repositorios y servicios); no requiere base de datos ni servicio de reconocimiento reales. Residente y residencia de prueba preconfigurados en los *mocks*. |
| **Condiciones/entradas que se varían** | Estado de presencia del residente (dentro/fuera), tiempo transcurrido desde la última marca, tipo del evento anterior, resultado de la verificación facial, red (SSID/BSSID) de origen. |
| **Resultado esperado (general)** | Cada marca genera el evento correcto (ENTRADA, SALIDA o INTENTO_FALLIDO), actualiza la presencia, señala anomalías y rechaza las marcas inválidas por rostro o por red. |
| **Casos asociados** | PU-01, PU-02, PU-03, PU-04, PU-05, PU-06, PU-07 |
| **Requisitos verificados** | RF-11, RF-12, RF-14, RF-16, RF-17, RF-18, RF-19, RN-01, RN-02 |

---

## ESC-U-02 · Autenticación y seguridad de acceso

| Campo | Descripción |
|-------|-------------|
| **Objetivo** | Verificar que el inicio de sesión concede el acceso solo con credenciales válidas y aplica las protecciones de seguridad: conteo y bloqueo por intentos fallidos, cuentas bloqueadas o desactivadas y sesión única por usuario. |
| **Componente bajo prueba** | `BusinessUser` — método `login` (capa de negocio del backend). |
| **Precondiciones** | Repositorios simulados con *mocks*; codificador de contraseñas (BCrypt) real. Cuentas de prueba en distintos estados (activa, bloqueada, desactivada). |
| **Condiciones/entradas que se varían** | Correspondencia de la contraseña, número de intentos fallidos acumulados, estado de la cuenta, inicio de sesión desde un segundo dispositivo. |
| **Resultado esperado (general)** | Con credenciales correctas entrega el token y reinicia los intentos; con incorrectas los contabiliza y, al quinto, bloquea; nunca permite el acceso a cuentas bloqueadas o desactivadas; un nuevo inicio invalida las sesiones anteriores. |
| **Casos asociados** | PU-08, PU-09, PU-10, PU-11, PU-12, PU-13 |
| **Requisitos verificados** | RF-01 (autenticación), RNF-02 (BCrypt), RNF-03 (bloqueo por intentos fallidos) |

---

## ESC-U-03 · Generación de nombres de documentos

| Campo | Descripción |
|-------|-------------|
| **Objetivo** | Verificar que los archivos de documentos se nombran de forma legible y segura para el sistema de archivos, según el patrón `TIPO_Nombre_Apellido_fecha`, sin colisiones ni caracteres inválidos. |
| **Componente bajo prueba** | `DocumentNameHelper` (utilidad del backend). |
| **Precondiciones** | Ninguna; es lógica pura sin dependencias externas. |
| **Condiciones/entradas que se varían** | Nombre con o sin tildes/espacios, existencia previa de un archivo con el mismo nombre, presencia o ausencia de extensión, datos del residente incompletos. |
| **Resultado esperado (general)** | Nombre normalizado conforme al patrón; se sustituyen caracteres no válidos; se agrega un sufijo numérico para evitar sobrescritura; no quedan puntos sueltos; ante datos faltantes se usa un marcador sin fallar. |
| **Casos asociados** | PU-14, PU-15, PU-16, PU-17, PU-18 |
| **Requisitos verificados** | RNF-12 (nombres de archivo legibles) |

---

## ESC-U-04 · Respaldo automático de archivos

| Campo | Descripción |
|-------|-------------|
| **Objetivo** | Verificar que el servicio de respaldo comprime correctamente fotos y documentos, tolera la ausencia de carpetas y aplica la política de retención eliminando los respaldos vencidos. |
| **Componente bajo prueba** | `BackupService` (servicio del backend). |
| **Precondiciones** | Sistema de archivos temporal simulado; carpetas y respaldos de prueba creados para el caso. |
| **Condiciones/entradas que se varían** | Existencia de la carpeta de archivos, antigüedad de los respaldos frente al periodo de conservación. |
| **Resultado esperado (general)** | Se genera un `.zip` conservando la estructura de carpetas; la falta de una carpeta no interrumpe el proceso; los respaldos más antiguos que el periodo de retención se eliminan. |
| **Casos asociados** | PU-19, PU-20, PU-21 |
| **Requisitos verificados** | RF-35 (respaldos automáticos) |

---

## ESC-U-05 · Arranque del sistema

| Campo | Descripción |
|-------|-------------|
| **Objetivo** | Verificar que la aplicación levanta correctamente su contexto (configuración, componentes y conexiones), como prueba de humo del ensamblaje del sistema. |
| **Componente bajo prueba** | Contexto de la aplicación Spring Boot. |
| **Precondiciones** | Base de datos MySQL encendida y configuración de prueba disponible. |
| **Condiciones/entradas que se varían** | — (verificación única de arranque). |
| **Resultado esperado (general)** | El contexto de Spring se carga sin errores; todos los componentes se inicializan y sus dependencias se resuelven. |
| **Casos asociados** | PU-22 |
| **Requisitos verificados** | RNF-15 (arranque automático del servidor); integridad del ensamblaje |

---

## ESC-U-06 · Cálculo de la fecha local en reportes

| Campo | Descripción |
|-------|-------------|
| **Objetivo** | Verificar que la fecha usada para filtrar reportes se calcula con la hora **local** del dispositivo y no en UTC, evitando que un reporte "diario" salga vacío o desfasado un día. |
| **Componente bajo prueba** | Utilidad de fecha del frontend (Angular). |
| **Precondiciones** | Entorno de prueba de Vitest; reloj simulado para fijar distintas horas del día. |
| **Condiciones/entradas que se varían** | Hora del día (noche, madrugada), formato de salida. |
| **Resultado esperado (general)** | La fecha resultante coincide con la fecha local del dispositivo, con mes y día rellenados a dos dígitos, sin adelantarse ni retroceder por conversión horaria. |
| **Casos asociados** | PU-23, PU-24, PU-25, PU-26 |
| **Requisitos verificados** | RF-36 — filtros de asistencia por rango de fechas (regresión del defecto de zona horaria) |

---

## ESC-U-07 · Almacenamiento de la sesión según el rol

| Campo | Descripción |
|-------|-------------|
| **Objetivo** | Verificar que la sesión se guarda con la persistencia adecuada a cada rol (residente persistente, administrador solo por sesión), que el token se lee correctamente y que el cierre de sesión limpia todo rastro. |
| **Componente bajo prueba** | Utilidad de almacenamiento de sesión del frontend (Angular). |
| **Precondiciones** | Entorno de prueba de Vitest con `localStorage`/`sessionStorage` simulados. |
| **Condiciones/entradas que se varían** | Rol del usuario, almacén donde reside el token, cambio de tipo de sesión, cierre de sesión, forma de la respuesta del backend. |
| **Resultado esperado (general)** | El residente persiste su sesión y el administrador no; el token se recupera sin importar el almacén; al cambiar de sesión no quedan copias viejas; el cierre limpia ambos almacenes; las respuestas del backend se interpretan correctamente. |
| **Casos asociados** | PU-27, PU-28, PU-29, PU-30, PU-31, PU-32, PU-33 |
| **Requisitos verificados** | RF-08 (persistencia distinta por rol), RF-07 (sesión única) |

---

## ESC-U-08 · Registro del motivo de asistencia

| Campo | Descripción |
|-------|-------------|
| **Objetivo** | Verificar que el motivo de la marca se gestiona según la regla de negocio: opcional en la entrada (con valor por defecto) y obligatorio en la salida, incluyendo la opción "Otros" con texto libre. |
| **Componente bajo prueba** | Lógica de la vista del residente (frontend Angular). |
| **Precondiciones** | Entorno de prueba de Vitest; formulario de marcado inicializado. |
| **Condiciones/entradas que se varían** | Tipo de marca (entrada/salida), motivo elegido o ausente, selección de "Otros" con texto libre. |
| **Resultado esperado (general)** | La entrada sin motivo asume "Retorno a la residencia"; se respeta el motivo elegido; la opción "Otros" usa el texto libre sin espacios sobrantes; la salida no envía un motivo inventado (el formulario lo exige). |
| **Casos asociados** | PU-34, PU-35, PU-36, PU-37, PU-38 |
| **Requisitos verificados** | RF-13 (motivo de salida obligatorio) |

---

## ESC-U-09 · Aislamiento del perfil entre sesiones

| Campo | Descripción |
|-------|-------------|
| **Objetivo** | Verificar que los datos del usuario no sobreviven a un cambio de sesión, de modo que quien entra después nunca vea el perfil del anterior. |
| **Componente bajo prueba** | Servicio de perfil del frontend (señal en memoria) y las operaciones de inicio y cierre de sesión. |
| **Precondiciones** | Entorno de prueba de Vitest; señal de perfil recreada antes de cada caso. |
| **Condiciones/entradas que se varían** | Perfil presente o ausente en memoria, y la operación aplicada: cerrar sesión, iniciar sesión, o ninguna. |
| **Resultado esperado (general)** | Tanto cerrar como iniciar sesión vacían el perfil en memoria; al abrir la pantalla, el nuevo usuario obtiene sus propios datos del servidor. El último caso comprueba, a la inversa, que sin esa limpieza sí se mostrarían datos ajenos. |
| **Casos asociados** | PU-39, PU-40, PU-41, PU-42 |
| **Requisitos verificados** | RF-06 (pantalla de perfil del residente), RF-07, RF-08 |

> Escenario de **regresión**: nace de un fallo hallado en pruebas de uso. El perfil se
> guarda en una señal en memoria y al cerrar sesión se limpiaban el token y el usuario,
> pero no esa señal; como la pantalla solo consulta al servidor cuando está vacía, tras
> usar el sistema como administrador y entrar luego como residente, el perfil del
> residente mostraba el correo del administrador.

---

## Nota para la matriz de trazabilidad (7.7)

La columna **Requisitos verificados** de cada escenario permite construir la matriz de
trazabilidad de forma directa: por cada requisito, los escenarios (y, dentro de ellos,
los casos PU-XX) que lo cubren. Un requisito puede aparecer en más de un escenario.
