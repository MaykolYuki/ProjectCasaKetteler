# 5.4 Gestión de Riesgos

Identifica, analiza y prioriza los riesgos del sistema **Casa Ketteler** (control de
asistencia por reconocimiento facial en una residencia universitaria) y define las
estrategias para reducir su probabilidad o su impacto.

---

## 5.4.1 Identificación de Riesgos

Se identificaron **12 riesgos** agrupados por naturaleza: técnicos, de seguridad,
operativos y de gestión.

| ID | Riesgo | Categoría | Descripción |
|----|--------|-----------|-------------|
| R-01 | Falsos negativos del reconocimiento facial | Técnico | Cambios de apariencia, mala iluminación o cámara deficiente impiden que un residente legítimo marque asistencia. |
| R-02 | Suplantación de identidad (*spoofing*) | Seguridad | Un tercero intenta marcar con una foto o video del residente. |
| R-03 | Cambio de router/punto de acceso invalida el BSSID | Operativo | Al reemplazar el equipo Wi-Fi, el BSSID guardado deja de coincidir y **nadie** puede marcar hasta reconfigurarlo (paso manual). |
| R-04 | Pérdida de datos por avería del disco | Operativo | Los respaldos se guardan en el mismo disco que el sistema; una falla física los perdería junto con los datos. |
| R-05 | Servidor caído en horario de marcas | Operativo | El equipo servidor se apaga, reinicia o no arranca durante las horas de entrada/salida. |
| R-06 | Saturación del pool de conexiones | Técnico | Bajo concurrencia muy alta, el límite de 10 conexiones a la BD (HikariCP) encola peticiones y degrada la respuesta. |
| R-07 | Cambio de IP del servidor | Operativo | Si cambia la IP del servidor, las apps móviles dejan de encontrarlo y hay que reinstalarlas. |
| R-08 | Exposición del `JWT_SECRET` | Seguridad | La clave de firma de tokens quedó en el historial de git; si se reutiliza en producción, se podrían falsificar sesiones. |
| R-09 | Tráfico sin cifrar (sin HTTPS) | Seguridad | En la red local el tráfico viaja en claro; podría interceptarse credenciales o datos. |
| R-10 | Dependencia de una sola persona (*bus factor*) | Gestión | La operación y el mantenimiento recaen en una única persona con el conocimiento técnico. |
| R-11 | Fuga o acceso indebido a datos personales | Seguridad | Fotografías y documentos de los residentes son datos sensibles; un acceso indebido tendría impacto legal y de privacidad. |
| R-12 | Crecimiento por encima de la capacidad probada | Técnico | Un número de residentes muy superior a 200 podría degradar el rendimiento no verificado. |

---

## 5.4.2 Análisis de Riesgo

### 5.4.2.1 Análisis Cualitativo

Cada riesgo se valora según su **probabilidad** de ocurrencia y su **impacto** si ocurre.

**Escala de probabilidad:** Muy baja (1) · Baja (2) · Media (3) · Alta (4) · Muy alta (5).
**Escala de impacto:** Insignificante (1) · Menor (2) · Moderado (3) · Mayor (4) · Crítico (5).

| ID | Probabilidad | Impacto | Justificación |
|----|--------------|---------|---------------|
| R-01 | Alta | Moderado | Ocurre con cierta frecuencia, pero hay reintento y actualización de fotos. |
| R-02 | Baja | Mayor | Mitigado por *liveness*, pero de materializarse comprometería la confianza del sistema. |
| R-03 | Media | Mayor | Cambiar el router no es frecuente, pero deja a todos sin marcar. |
| R-04 | Media | Crítico | Falla de disco poco frecuente, pero implicaría pérdida total de datos. |
| R-05 | Media | Mayor | Cortes de luz o reinicios pueden coincidir con horas pico. |
| R-06 | Baja | Moderado | El uso real (≈40 residentes) está muy por debajo del punto de saturación. |
| R-07 | Media | Moderado | Puede pasar al cambiar de red; obliga a reinstalar la app. |
| R-08 | Media | Mayor | La clave está en el historial; el riesgo depende de rotarla en producción. |
| R-09 | Baja | Moderado | Requiere acceso físico a la red local para interceptar. |
| R-10 | Media | Moderado | Habitual en proyectos pequeños; afecta la continuidad. |
| R-11 | Baja | Crítico | Baja probabilidad en red local, pero impacto legal/privacidad alto. |
| R-12 | Baja | Moderado | El crecimiento a >200 residentes es poco probable a corto plazo. |

### 5.4.2.2 Análisis Cuantitativo

Se asigna un valor numérico a cada escala y se calcula la **exposición al riesgo**:

> **Exposición = Probabilidad (1–5) × Impacto (1–5)**

Niveles: **Bajo** (1–6) · **Medio** (8–12) · **Alto** (15–25).

| ID | Probabilidad (P) | Impacto (I) | Exposición (P × I) | Nivel |
|----|:---:|:---:|:---:|:-----:|
| R-04 | 3 | 5 | **15** | 🔴 Alto |
| R-01 | 4 | 3 | **12** | 🟡 Medio |
| R-03 | 3 | 4 | **12** | 🟡 Medio |
| R-05 | 3 | 4 | **12** | 🟡 Medio |
| R-08 | 3 | 4 | **12** | 🟡 Medio |
| R-11 | 2 | 5 | **10** | 🟡 Medio |
| R-07 | 3 | 3 | **9** | 🟡 Medio |
| R-10 | 3 | 3 | **9** | 🟡 Medio |
| R-02 | 2 | 4 | **8** | 🟡 Medio |
| R-06 | 2 | 3 | **6** | 🟢 Bajo |
| R-09 | 2 | 3 | **6** | 🟢 Bajo |
| R-12 | 2 | 3 | **6** | 🟢 Bajo |

---

## 5.4.3 Clasificación de Riesgos

| Categoría | Riesgos | Observación |
|-----------|---------|-------------|
| **Técnicos** | R-01, R-06, R-12 | Ligados al reconocimiento facial y al rendimiento. |
| **De seguridad** | R-02, R-08, R-09, R-11 | Identidad, credenciales, cifrado y datos personales. |
| **Operativos** | R-03, R-04, R-05, R-07 | Continuidad del servicio e infraestructura. |
| **De gestión** | R-10 | Continuidad del conocimiento y el soporte. |

---

## 5.4.4 Matriz de Probabilidad e Impacto

Ubicación de cada riesgo según su probabilidad (filas) e impacto (columnas). El color
indica el nivel de exposición: 🟢 Bajo · 🟡 Medio · 🔴 Alto.

| Probabilidad ↓ / Impacto → | 1 Insignif. | 2 Menor | 3 Moderado | 4 Mayor | 5 Crítico |
|----------------------------|:----------:|:-------:|:----------:|:-------:|:---------:|
| **5 Muy alta** | 🟢 | 🟡 | 🟡 | 🔴 | 🔴 |
| **4 Alta** | 🟢 | 🟡 | 🟡 **R-01** | 🔴 | 🔴 |
| **3 Media** | 🟢 | 🟡 | 🟡 **R-07, R-10** | 🟡 **R-03, R-05, R-08** | 🔴 **R-04** |
| **2 Baja** | 🟢 | 🟢 | 🟢 **R-06, R-09, R-12** | 🟡 **R-02** | 🟡 **R-11** |
| **1 Muy baja** | 🟢 | 🟢 | 🟢 | 🟡 | 🟡 |

---

## 5.4.5 Priorización de Riesgos

Orden de atención según la exposición (de mayor a menor). Los de nivel Alto y Medio-alto
requieren acción prioritaria.

| Prioridad | ID | Riesgo | Exposición | Nivel |
|:---------:|----|--------|:----------:|:-----:|
| 1 | R-04 | Pérdida de datos por avería del disco | 15 | 🔴 Alto |
| 2 | R-01 | Falsos negativos del reconocimiento | 12 | 🟡 Medio |
| 3 | R-03 | Cambio de router invalida el BSSID | 12 | 🟡 Medio |
| 4 | R-05 | Servidor caído en horario de marcas | 12 | 🟡 Medio |
| 5 | R-08 | Exposición del `JWT_SECRET` | 12 | 🟡 Medio |
| 6 | R-11 | Fuga de datos personales | 10 | 🟡 Medio |
| 7 | R-07 | Cambio de IP del servidor | 9 | 🟡 Medio |
| 8 | R-10 | Dependencia de una sola persona | 9 | 🟡 Medio |
| 9 | R-02 | Suplantación (*spoofing*) | 8 | 🟡 Medio |
| 10 | R-06 | Saturación del pool de conexiones | 6 | 🟢 Bajo |
| 11 | R-09 | Tráfico sin HTTPS | 6 | 🟢 Bajo |
| 12 | R-12 | Crecimiento sobre la capacidad probada | 6 | 🟢 Bajo |

---

## 5.4.6 Estrategias de Mitigación

Para cada riesgo se indica la **estrategia** (Evitar, Mitigar, Transferir o Aceptar), los
**controles ya implementados** y las **acciones recomendadas**.

| ID | Estrategia | Controles actuales | Acciones recomendadas |
|----|-----------|--------------------|------------------------|
| **R-04** | Mitigar | Respaldos automáticos (BD diario, archivos semanal, retención 14 días) | Copiar periódicamente la carpeta `backups` a **USB o nube** (tarea semanal ya documentada en el manual de operación). |
| **R-01** | Mitigar | Ráfaga de 3 fotos + mediana; umbral calibrado; reintento inmediato | Actualizar las fotos de referencia cuando cambie la apariencia; guía al residente (buena luz, de frente, sin gorra/lentes). |
| **R-03** | Mitigar | Validación por BSSID/SSID; procedimiento manual documentado (DESPLIEGUE 5.1) | Ante cambio de router, actualizar el BSSID; en residencias con varios AP, validar por **SSID**. |
| **R-05** | Mitigar | Arranque automático (Task Scheduler); ventana de estado | UPS (batería) para cortes de luz; verificar el arranque tras cada reinicio (mantenimiento mensual). |
| **R-08** | Evitar | Clave por variable de entorno; `.env` fuera de git | **Rotar el `JWT_SECRET`** en el despliegue de producción (distinto al de desarrollo). Ya documentado en DESPLIEGUE. |
| **R-11** | Mitigar | RBAC; filtrado por usuario; operación solo en red local | Restringir acceso físico al servidor; política de retención/borrado de datos de exresidentes; considerar cifrado en reposo. |
| **R-07** | Mitigar | IP documentada; advertencia en el manual | Reservar la IP en el router (DHCP estático) o usar un nombre de dominio local. |
| **R-10** | Mitigar | Documentación completa (instalación, despliegue, operación, arquitectura) | Capacitar a una segunda persona; mantener la documentación actualizada. |
| **R-02** | Mitigar | Detección de vida (*liveness / anti-spoofing*); umbral sobre la mediana | Revisar periódicamente los eventos marcados como anomalía; auditar intentos fallidos. |
| **R-06** | Aceptar | Pool HikariCP de 10; degradación sin errores hasta 800 usuarios (probado en 8.3) | Monitorear; ampliar el pool solo si el uso real se acerca al límite. |
| **R-09** | Mitigar | Operación en red local cerrada | Habilitar **HTTPS** al publicar el sistema fuera de la red local. |
| **R-12** | Aceptar | Estabilidad verificada hasta 200 residentes (RNF-07) | Repetir las pruebas de carga si el número de residentes se aproxima al límite. |

---

## Conclusión

La mayoría de los riesgos son de nivel **Medio** o **Bajo** y cuentan con controles ya
implementados. El único riesgo **Alto** (R-04, pérdida de datos) se reduce de forma
sencilla con la copia externa periódica de los respaldos. Los riesgos de seguridad
restantes (rotar el `JWT_SECRET`, habilitar HTTPS) se materializan solo en un despliegue
público y ya están contemplados en la documentación de despliegue.
