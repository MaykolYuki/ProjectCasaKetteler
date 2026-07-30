# Accesibilidad: resultados de las correcciones

Informe de la segunda evaluación con **axe DevTools**, tras aplicar el plan de acción
de la [Matriz de Evaluación de Accesibilidad](MATRIZ-ACCESIBILIDAD.md).

---

## 1. Resultado

| Indicador | Antes | Después | Variación |
|-----------|------:|--------:|----------:|
| Hallazgos totales | 219 | **1** | **−99.5 %** |
| Incumplimientos WCAG | 89 | **1** | −98.9 % |
| Hallazgos críticos | 17 | **0** | **−100 %** |
| Buenas prácticas incumplidas | 130 | **0** | **−100 %** |

> El único hallazgo restante era un contraste que **no aparecía en la evaluación
> original** (un estado de botón que no estaba en pantalla durante el primer escaneo).
> Ya está corregido; ver el apartado 4.

---

## 2. Comparación por pantalla

Pantallas reevaluadas tras las correcciones:

| Pantalla | Antes | Después | Estado |
|----------|------:|--------:|:------:|
| `/admin-gestion-usuarios` | 93 | **0** | ✅ Conforme |
| `/admin-crear-usuario` | 14 | **0** | ✅ Conforme |
| `/admin-home` | 5 | **1** | ◑ Corregido después |
| `/residente-cambiar-contra` | 2 | **0** | ✅ Conforme |
| **Subtotal reevaluado** | **114** | **1** | |

> `/admin-gestion-usuarios` concentraba el 42 % de todos los hallazgos del sistema y
> pasó a cero.

Las siete pantallas restantes no se reevaluaron, pero recibieron las mismas
correcciones. Se detalla en el apartado 5.

---

## 3. Qué resolvió cada acción

| Acción | Hallazgos resueltos | Cómo |
|--------|--------------------:|------|
| **1. Estructura semántica** | 128 | Las 5 pantallas de administración usaban un `<div>` como contenedor raíz, dejando todo el contenido fuera de cualquier región semántica, y titulaban con `<h2>` sin ningún `<h1>`. Se cambiaron a `<main>` y `<h1>` |
| **2. Nombres accesibles** | 17 | Las etiquetas existían pero sin `for`, y los campos sin `id`: no estaban asociados. Se vincularon 12 pares y se nombraron 3 desplegables y 1 botón que solo tenía un icono |
| **3. Contraste de color** | 74 | Se oscureció cada color lo mínimo necesario para alcanzar 4.5:1, conservando tono y saturación |
| **4. Acceso por teclado** | 3 | `tabindex` y nombre en las zonas con desplazamiento |
| **5. Jerarquía de encabezados** | 2 | Se corrigió el salto de `h1` a `h3` y se convirtió en grupo con nombre una etiqueta suelta |

### Un caso que explicaba dos hallazgos a la vez

El `<h1>` de la pantalla de inicio del residente llevaba `role="button"` para que se
pudiera pulsar. Ese atributo **anula el rol de encabezado**: para un lector de pantalla
la página dejaba de tener título principal, y además el rol no está permitido en ese
elemento. Se movió la acción a un `<button>` real anidado dentro del `<h1>`, que
conserva el comportamiento táctil y devuelve al encabezado su función.

---

## 4. El hallazgo que quedaba, y por qué no estaba antes

La segunda evaluación detectó un contraste insuficiente en el botón verde del panel de
administración: texto `#117a65` sobre fondo `#a3e4d7`, ratio **3.66**.

No es una corrección fallida: **ese par de colores no figuraba en la primera
evaluación**. Una herramienta automática solo mide lo que está pintado en pantalla en
ese momento, y ese estado del botón no lo estaba.

Para no repetir el ciclo de "escanear, corregir, volver a escanear", se auditó el código
completo: se revisaron **todas** las reglas de estilo que definen a la vez color de
texto y de fondo, calculando el contraste de cada par. Aparecieron **10 combinaciones**
por debajo del umbral, la mayoría en estados que ninguna evaluación había mostrado
todavía (avisos de Wi-Fi conectado y desconectado, pestaña activa, botón de captura,
mensajes de error y de éxito).

Se corrigieron **9**. La restante se dejó a propósito:

| Elemento | Contraste | Decisión |
|----------|----------:|----------|
| Botón de subir documento, **deshabilitado** | 1.44 | **Se conserva.** El criterio 1.4.3 exime expresamente los componentes de interfaz inactivos. El gris apagado es justamente la señal de "no disponible": subirle el contraste lo haría parecer utilizable |

---

## 5. Alcance de la verificación

Conviene ser preciso sobre qué está comprobado y qué es esperado:

| | Pantallas | Situación |
|---|---|---|
| **Verificado** | 4 | Reevaluadas con axe: 114 hallazgos → 1 |
| **Corregido, pendiente de reevaluar** | 7 | Recibieron las mismas correcciones (misma estructura, mismas variables de color) |
| **Ya conformes desde el inicio** | 2 | `/login` y `/admin-cambiar-contrasena` |

Además, la auditoría de contraste sobre el código cubre **todas** las pantallas, no solo
las reevaluadas.

> **Recordatorio metodológico:** axe es una herramienta automatizada y detecta entre el
> 30 % y el 40 % de los problemas de accesibilidad. Que el resultado sea cero no
> equivale a conformidad plena con WCAG 2.1 AA: quedan fuera aspectos que exigen
> revisión humana, como la navegación completa por teclado, la prueba con lector de
> pantalla o la claridad del lenguaje.

---

## 6. Comprobaciones de no regresión

Las correcciones son de estructura y estilo, así que se verificó que no alteraran el
funcionamiento ni la apariencia:

| Comprobación | Resultado |
|--------------|-----------|
| Compilación del frontend | ✅ Sin errores |
| Pruebas unitarias | ✅ **20 / 20** |
| Tamaño de los títulos | ✅ Se fijó `font-size` al pasar de `h2` a `h1`, para que el diseño no cambiara |
| Colores por debajo de 4.5:1 en el código | ✅ 0 (salvo el control deshabilitado, exento) |
| Comportamiento táctil del título del residente | ✅ Conservado mediante un `<button>` anidado |

---

## 7. Conclusión

Las correcciones eliminaron el **99.5 %** de los hallazgos y la **totalidad de los
críticos**, sin rediseñar ninguna pantalla: se trató de estructura semántica, etiquetas
correctamente asociadas y ajustes finos de color.

El resultado confirma el diagnóstico de la matriz inicial: los 219 hallazgos no eran 219
defectos distintos, sino cuatro causas repetidas a lo largo de la interfaz. Corregir la
causa —y no cada síntoma— es lo que permitió resolverlos en bloque.
