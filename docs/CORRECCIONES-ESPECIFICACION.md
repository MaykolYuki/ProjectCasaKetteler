# Revisión de la Especificación de Requisitos — Correcciones pendientes

Revisión del documento `ESPECIFICACIÓN DE REQUISITOS.docx` (versión actual, con la
numeración RF-01…RF-40 / RN-01…RN-03 / RNF-01…RNF-23).

**Estado general:** el documento está **casi al día**. Ya incorpora la arquitectura de
servicio en el servidor, la sesión única, la persistencia por rol, el umbral del 55 %,
las anomalías, los respaldos automáticos y el reconocimiento en CPU. Quedan **6 ajustes**
antes de darlo por cerrado.

---

## 1. Requisitos que describen una arquitectura que NO existe (eliminar)

El sistema **no** guarda asistencia sin conexión ni hace "sincronización diferida":
funciona en la red local, procesando en el servidor en el momento. Estos dos requisitos
describen un modo *offline* que se descartó y contradicen a RF-39 y RF-40.

- **RN-03** — *"…lo almacenará de forma local y aplicará el proceso de sincronización
  diferida (RNF-13)…"*. **Eliminar.** (Además, su referencia a "RNF-13" está rota: en el
  documento actual RNF-13 es la disponibilidad del 95 %, no una sincronización.)

- **RNF-14** — *"El sistema almacenará los registros de asistencia localmente cuando no
  haya conexión a internet y los sincronizará automáticamente al restablecerse."*
  **Eliminar.** (Lo que sí aplica —operar sin Internet dentro de la red local— ya lo
  cubren **RF-20** y **RF-40**.)

---

## 2. Numeración duplicada (renumerar)

Hay **dos requisitos con el número RNF-22**:

- `RNF-22` (Fiabilidad): *liveness / anti-spoofing…* → **se queda como RNF-22**.
- `RNF-22` (el último): *"El sistema notificará oportunamente al usuario cuando el
  servidor no responda…"* → **renumerar a RNF-24**.

---

## 3. Texto sobrante (limpiar)

- **RNF-02** termina con la frase suelta *"Personal de los residentes."* — es un residuo
  de copiado. **Borrar** esa frase. Debe quedar:

  > **RNF-02:** El sistema aplicará el algoritmo BCrypt para el cifrado de contraseñas.
  > En el despliegue en producción se habilitará HTTPS para la transmisión de datos
  > sensibles.

---

## 4. Requisito no implementado (reformular o mover a "trabajo futuro")

- **RNF-23** — *"…restringirá mediante permisos de base de datos la modificación directa
  de registros de asistencia, permitiéndolo únicamente a través de funciones
  administrativas auditadas."*

  Hoy **no** existe restricción a nivel de permisos de la base de datos ni bitácora de
  auditoría. Lo que sí hay es control de acceso por roles (RBAC) y filtrado por usuario
  (ya descritos en RNF-01 y RNF-04). Opciones:
  - **(a)** Moverlo a una sección de *mejoras futuras*, o
  - **(b)** Reformularlo a lo que sí se cumple:

    > **RNF-23 (reformulado):** El sistema restringirá la modificación de los registros
    > de asistencia a las funciones administrativas del backend, protegidas por control
    > de acceso basado en roles (RBAC); no expondrá endpoints de edición directa de
    > eventos de asistencia.

---

## 5. Aviso importante: la numeración de los Escenarios (7.5) NO coincide

El **Diseño de Escenarios** ya pegado en el documento usa números de requisito de un
borrador anterior. Con la numeración **actual** del .docx, la columna *"Requisitos
verificados"* de cada escenario debe quedar así:

| Escenario | Dice (borrador viejo) | Debe decir (numeración actual) |
|-----------|-----------------------|-------------------------------|
| ESC-U-01 Registro de asistencia | RF-08, RF-09, RF-13, RF-38, RN-01, RN-02 | **RF-11, RF-12, RF-14, RF-16, RF-17, RN-01, RN-02** |
| ESC-U-02 Autenticación y seguridad | RF-36, RNF-02 | **RF-01, RF-07, RNF-02, RNF-03** |
| ESC-U-03 Nombres de documentos | RNF-23 | **RNF-12** |
| ESC-U-04 Respaldo de archivos | RF-39 | **RF-35** |
| ESC-U-05 Arranque del sistema | (infraestructura) | (infraestructura) — sin cambio |
| ESC-U-06 Fecha local en reportes | RI (reportes) | **RF-36** |
| ESC-U-07 Almacenamiento de sesión | RF-37, RF-36 | **RF-08, RF-07** |
| ESC-U-08 Motivo de asistencia | RF-10 | **RF-13** |

> La **Matriz de Trazabilidad** (`MATRIZ-TRAZABILIDAD.md`) ya está construida con la
> numeración **actual**, así que sirve de referencia para dejar estos números iguales
> en todo el informe.

---

## Resumen de acciones

| # | Acción | Requisito |
|---|--------|-----------|
| 1 | Eliminar | RN-03 |
| 2 | Eliminar | RNF-14 |
| 3 | Renumerar a RNF-24 | el segundo RNF-22 (servidor no responde) |
| 4 | Borrar frase sobrante | RNF-02 |
| 5 | Reformular o mover a futuros | RNF-23 |
| 6 | Corregir números de requisito | Escenarios ESC-U-01 … ESC-U-08 (ver tabla) |
