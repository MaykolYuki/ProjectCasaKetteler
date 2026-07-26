# Especificación de Requisitos — Texto actualizado

Redacción lista para pegar en el documento oficial. Está organizada en tres bloques:

1. **Requisitos a REEMPLAZAR** — cambia el texto viejo por el de aquí (mismo número).
2. **Requisitos a ELIMINAR / replantear** — la sección de "Procesamiento Local".
3. **Requisitos NUEVOS** — agregar al final de cada categoría.

> Los requisitos que **no** aparecen en este documento se mantienen **sin cambios**.

---

## 1. Requisitos a REEMPLAZAR

Sustituye cada uno por el texto siguiente (conserva el mismo número):

**RF-06.** El sistema mostrará al residente una pantalla de perfil desde la cual podrá
visualizar sus datos personales y su fotografía de referencia.

**RF-08.** El sistema capturará una ráfaga de imágenes del rostro del residente y las
enviará al servicio de reconocimiento facial, el cual calculará el nivel de coincidencia
contra el patrón facial almacenado tomando la mediana de las capturas válidas, de modo
que una imagen defectuosa no altere el resultado.

**RF-09.** El sistema almacenará cada marca como un evento independiente con los
siguientes datos: fecha y hora del evento, tipo de evento (entrada, salida o intento
fallido), identificador del residente, motivo (obligatorio en salida), nivel de
coincidencia facial, datos de la red (SSID/BSSID) e IP del dispositivo.

**RF-10.** El sistema exigirá el motivo de salida mediante una lista desplegable con
opciones predefinidas, incluyendo una opción "Otros" que habilite un campo de texto
para detallarlo. En los registros de entrada el motivo será opcional; de omitirse, se
registrará automáticamente como "Retorno a la residencia".

**RF-12.** El sistema desplegará una notificación visual en la pantalla del dispositivo
confirmando el resultado del registro de asistencia, diferenciando por color los casos
de éxito y de error.

**RF-13.** El sistema habilitará el registro de asistencia únicamente cuando la mediana
del nivel de coincidencia facial de la ráfaga sea igual o superior al 55 %, umbral
calibrado con datos reales, y rechazará los intentos que no alcancen dicho valor.

**RF-15.** El sistema leerá el BSSID (identificador del punto de acceso) de la red WiFi
conectada y lo comparará con el registrado para la residencia a fin de validarla; en su
defecto, utilizará el SSID como criterio alternativo.

**RN-01.** El sistema habilitará el registro de asistencia exclusivamente cuando el
dispositivo del residente esté conectado a la red WiFi oficial de la residencia,
verificada preferentemente por su BSSID (o por su SSID en su defecto), bloqueando
cualquier intento desde redes externas.

**RN-02.** El sistema garantizará que la asistencia corresponde al residente real
mediante la verificación facial: la captura debe coincidir con el patrón facial del
residente indicado, impidiendo que un usuario registre asistencia en nombre de otro.

**RNF-02.** El sistema aplicará el algoritmo BCrypt para el cifrado de contraseñas. En
el despliegue en producción se habilitará HTTPS para la transmisión de datos sensibles.

**RNF-05.** El sistema completará el procesamiento del reconocimiento facial en un tiempo
aproximado de 3 a 5 segundos por marca, ejecutándose en el procesador (CPU) del servidor
de la residencia.

**RNF-20.** El sistema aplicará técnicas de detección de vida (*liveness / anti-spoofing*)
y un umbral de confianza sobre la mediana de la ráfaga para reducir falsos positivos y
negativos.

---

## 2. Requisitos a ELIMINAR o replantear

La sección **"Procesamiento Local"** describía una arquitectura en la que el
reconocimiento corría en el dispositivo y funcionaba sin conexión. Esa arquitectura se
reemplazó por un **servicio de reconocimiento en el servidor**, dentro de la red local.

**Eliminar:** RF-35 · RN-03 · RNF-13 (describen operación offline y sincronización
diferida que ya no existen).

**Reemplazar RF-34 por:**

> **RF-34.** El sistema procesará el reconocimiento facial en un servicio dedicado
> alojado en el servidor de la residencia, dentro de la red local, sin enviar las
> imágenes a servicios externos de terceros.

**Agregar en su lugar:**

> **RF-35 (nuevo).** El sistema operará dentro de la red local de la residencia. El
> reconocimiento facial no requerirá conexión a Internet, pero sí que el dispositivo del
> residente se encuentre en la misma red local que el servidor.

> *(Nota para el equipo: con esto, la asistencia es estrictamente presencial y en sitio,
> lo cual es coherente con el propósito del sistema.)*

---

## 3. Requisitos NUEVOS (agregar)

Funcionalidades ya implementadas que conviene incorporar a la especificación.

### En "Gestión de Usuarios / Seguridad"

**RF-36.** El sistema permitirá una única sesión activa por usuario: al iniciar sesión,
invalidará automáticamente las sesiones abiertas previamente en otros dispositivos.

**RF-37.** El sistema mantendrá la sesión iniciada del residente en su dispositivo
personal, permitiéndole el acceso directo posterior; en cambio, la sesión del
administrador expirará al cerrar el navegador, por tratarse de un equipo compartido.

**RF-40.** El sistema proveerá el acceso del administrador mediante un navegador web; la
interfaz será servida por el propio backend, sin requerir la instalación de un servidor
web adicional.

### En "Control de Asistencia"

**RF-38.** El sistema marcará como anomalía todo evento de asistencia del mismo tipo que
el evento inmediatamente anterior (por ejemplo, dos entradas sin una salida intermedia),
dejándolo señalado para revisión administrativa.

### En "Gestión Documental / Reportes"

**RF-39.** El sistema generará respaldos automáticos de la base de datos (con frecuencia
diaria) y de los archivos de fotos y documentos (con frecuencia semanal), conservando
los respaldos de los últimos 14 días, y permitirá al administrador generar un respaldo
bajo demanda.

### Nuevos requisitos no funcionales

**RNF-22.** El sistema notificará oportunamente al usuario cuando el servidor no
responda, evitando esperas indefinidas ante una falta de conexión.

**RNF-23.** El sistema almacenará los documentos con nombres de archivo legibles, según
el patrón `TIPO_Nombre_Apellido_fecha`.

**RNF-24.** El sistema se iniciará automáticamente al encender el equipo servidor de la
residencia, quedando operativo sin intervención manual.

---

## Resumen de acciones para el redactor

| Acción | Requisitos |
|--------|-----------|
| **Reemplazar texto** | RF-06, RF-08, RF-09, RF-10, RF-12, RF-13, RF-15, RN-01, RN-02, RNF-02, RNF-05, RNF-20 |
| **Eliminar** | RF-35, RN-03, RNF-13 |
| **Reemplazar/replantear** | RF-34 (y nuevo RF-35) |
| **Agregar (nuevos)** | RF-36, RF-37, RF-38, RF-39, RF-40, RNF-22, RNF-23, RNF-24 |
| **Sin cambios** | El resto |
