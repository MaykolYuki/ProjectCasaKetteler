# Matriz de Evaluación de Accesibilidad

Evaluación de la interfaz web del sistema **Casa Ketteler** frente a las Pautas de
Accesibilidad para el Contenido Web (**WCAG 2.1**, niveles A y AA).

> **Estado del documento:** retrato de la situación **ANTES** de aplicar correcciones.
> Sirve como línea base para medir la mejora una vez implementadas las acciones.

---

## 1. Ficha de la evaluación

| Elemento | Detalle |
|----------|---------|
| **Herramienta** | axe DevTools (extensión de navegador) |
| **Estándar evaluado** | WCAG 2.1 niveles A y AA |
| **Buenas prácticas** | Habilitadas (se reportan por separado) |
| **Sistema evaluado** | Interfaz web servida por el backend, `http://localhost:8001` |
| **Alcance** | 13 rutas de la aplicación (100 % de las pantallas) |
| **Fecha** | 30 de julio de 2026 |
| **Tipo de evaluación** | Automatizada |

> **Nota sobre el alcance de una evaluación automatizada:** una herramienta como axe
> detecta aproximadamente el 30–40 % de los problemas de accesibilidad. No sustituye la
> revisión manual (navegación por teclado, lectores de pantalla, comprensión del
> contenido), pero sí identifica de forma fiable los incumplimientos objetivos.

---

## 2. Resumen ejecutivo

| Indicador | Valor |
|-----------|------:|
| Pantallas evaluadas | **13 / 13** |
| Pantallas sin ningún hallazgo | **2** |
| Total de hallazgos | **219** |
| — Incumplimientos **WCAG** | **89** |
| — Buenas prácticas (no son incumplimiento) | **130** |
| Reglas distintas incumplidas | **11** |
| Criterios WCAG afectados | **5** |

**Por severidad:**

| Severidad | N.º | Proporción |
|-----------|----:|-----------:|
| Crítica | 17 | 8 % |
| Grave | 72 | 33 % |
| Moderada | 129 | 59 % |
| Menor | 1 | < 1 % |

> **Lectura clave:** de los 219 hallazgos, **130 son buenas prácticas** de axe y **no
> constituyen incumplimiento de WCAG**. Los incumplimientos reales son **89**, y se
> concentran en **dos causas**: contraste de color insuficiente (74) y controles de
> formulario sin etiqueta accesible (17).

---

## 3. Matriz de cobertura por pantalla

| ID | Pantalla | Ruta | Perfil | Hallazgos | Crít. | Grave | Mod. | Menor | WCAG | Estado |
|----|----------|------|--------|----------:|------:|------:|-----:|------:|-----:|:------:|
| P-01 | Inicio de sesión | `/login` | Ambos | 0 | 0 | 0 | 0 | 0 | 0 | ✅ Conforme |
| P-02 | Panel principal | `/admin-home` | Admin | 5 | 1 | 4 | 0 | 0 | 5 | ❌ No conforme |
| P-03 | Gestión de usuarios | `/admin-gestion-usuarios` | Admin | 93 | 0 | 18 | 75 | 0 | 18 | ❌ No conforme |
| P-04 | Crear usuario | `/admin-crear-usuario` | Admin | 14 | 5 | 0 | 9 | 0 | 5 | ❌ No conforme |
| P-05 | Editar usuario | `/admin-editar-usuario` | Admin | 19 | 7 | 0 | 12 | 0 | 7 | ❌ No conforme |
| P-06 | Documentos del residente | `/admin-documentos-usuario` | Admin | 24 | 0 | 2 | 22 | 0 | 2 | ❌ No conforme |
| P-07 | Reportes de asistencia | `/admin-reportes` | Admin | 17 | 3 | 5 | 9 | 0 | 8 | ❌ No conforme |
| P-08 | Cambiar contraseña | `/admin-cambiar-contrasena` | Admin | 0 | 0 | 0 | 0 | 0 | 0 | ✅ Conforme |
| P-09 | Inicio del residente | `/residente-home` | Residente | 7 | 1 | 4 | 1 | 1 | 5 | ❌ No conforme |
| P-10 | Perfil | `/residente-perfil` | Residente | 6 | 0 | 6 | 0 | 0 | 6 | ❌ No conforme |
| P-11 | Historial de asistencia | `/residente-historial` | Residente | 22 | 0 | 22 | 0 | 0 | 22 | ❌ No conforme |
| P-12 | Documentos | `/residente-documentos` | Residente | 10 | 0 | 9 | 1 | 0 | 9 | ❌ No conforme |
| P-13 | Cambiar contraseña | `/residente-cambiar-contra` | Residente | 2 | 0 | 2 | 0 | 0 | 2 | ❌ No conforme |
| | **TOTAL** | | | **219** | **17** | **72** | **129** | **1** | **89** | **2 / 13 conformes** |

---

## 4. Matriz de hallazgos por criterio WCAG

Agrupa los incumplimientos **reales** de la norma (excluye buenas prácticas).

| ID | Criterio WCAG | Nivel | Regla (axe) | Descripción del incumplimiento | Ocurrencias | Pantallas afectadas | Severidad | Estado |
|----|---------------|:-----:|-------------|--------------------------------|------------:|--------------------:|-----------|:------:|
| W-01 | **1.4.3** Contraste (mínimo) | AA | `color-contrast` | Texto con relación de contraste inferior a 4.5:1 respecto al fondo | 74 | 8 | Grave | ❌ |
| W-02 | **4.1.2** Nombre, función, valor | A | `label` | Campos de formulario sin etiqueta asociada | 13 | 3 | Crítica | ❌ |
| W-03 | **4.1.2** Nombre, función, valor | A | `select-name` | Listas desplegables sin nombre accesible | 3 | 2 | Crítica | ❌ |
| W-04 | **2.1.1** Teclado / **2.1.3** | A | `scrollable-region-focusable` | Zonas con desplazamiento inalcanzables por teclado | 3 | 3 | Grave | ❌ |
| W-05 | **4.1.2** Nombre, función, valor | A | `button-name` | Botón sin texto ni nombre accesible | 1 | 1 | Crítica | ❌ |
| W-06 | **1.3.1** Información y relaciones | AA | `heading-markup` | Texto con apariencia de encabezado sin marcarlo como tal | 1 | 1 | Grave | ❌ |
| | | | | **Total incumplimientos WCAG** | **95\*** | | | |

> \* La suma por criterio (95) supera el total de hallazgos WCAG (89) porque algunos
> hallazgos están etiquetados con más de un criterio (por ejemplo, `scrollable-region-focusable`
> afecta a 2.1.1 y 2.1.3 a la vez).

### Buenas prácticas (no son incumplimiento de WCAG)

| ID | Regla (axe) | Descripción | Ocurrencias | Pantallas | Severidad |
|----|-------------|-------------|------------:|----------:|-----------|
| B-01 | `region` | Contenido fuera de una región semántica (*landmark*) | 117 | 6 | Moderada |
| B-02 | `page-has-heading-one` | La página no tiene un encabezado de nivel 1 (`<h1>`) | 6 | 6 | Moderada |
| B-03 | `landmark-one-main` | La página no tiene una región principal (`<main>`) | 5 | 5 | Moderada |
| B-04 | `heading-order` | Los encabezados no siguen un orden jerárquico | 1 | 1 | Moderada |
| B-05 | `aria-allowed-role` | Rol ARIA no permitido para ese elemento | 1 | 1 | Menor |
| | | **Total buenas prácticas** | **130** | | |

---

## 5. Matriz detallada: pantalla × regla

| ID | Pantalla | Regla | Criterio | Nivel | Severidad | Ocurrencias |
|----|----------|-------|:--------:|:-----:|-----------|------------:|
| D-01 | `/admin-home` | `color-contrast` | 1.4.3 | AA | Grave | 4 |
| D-02 | `/admin-home` | `button-name` | 4.1.2 | A | Crítica | 1 |
| D-03 | `/admin-gestion-usuarios` | `region` | — | BP | Moderada | 73 |
| D-04 | `/admin-gestion-usuarios` | `color-contrast` | 1.4.3 | AA | Grave | 18 |
| D-05 | `/admin-gestion-usuarios` | `landmark-one-main` | — | BP | Moderada | 1 |
| D-06 | `/admin-gestion-usuarios` | `page-has-heading-one` | — | BP | Moderada | 1 |
| D-07 | `/admin-crear-usuario` | `region` | — | BP | Moderada | 7 |
| D-08 | `/admin-crear-usuario` | `label` | 4.1.2 | A | Crítica | 5 |
| D-09 | `/admin-crear-usuario` | `landmark-one-main` | — | BP | Moderada | 1 |
| D-10 | `/admin-crear-usuario` | `page-has-heading-one` | — | BP | Moderada | 1 |
| D-11 | `/admin-editar-usuario` | `region` | — | BP | Moderada | 10 |
| D-12 | `/admin-editar-usuario` | `label` | 4.1.2 | A | Crítica | 7 |
| D-13 | `/admin-editar-usuario` | `landmark-one-main` | — | BP | Moderada | 1 |
| D-14 | `/admin-editar-usuario` | `page-has-heading-one` | — | BP | Moderada | 1 |
| D-15 | `/admin-documentos-usuario` | `region` | — | BP | Moderada | 20 |
| D-16 | `/admin-documentos-usuario` | `color-contrast` | 1.4.3 | AA | Grave | 1 |
| D-17 | `/admin-documentos-usuario` | `scrollable-region-focusable` | 2.1.1 | A | Grave | 1 |
| D-18 | `/admin-documentos-usuario` | `landmark-one-main` | — | BP | Moderada | 1 |
| D-19 | `/admin-documentos-usuario` | `page-has-heading-one` | — | BP | Moderada | 1 |
| D-20 | `/admin-reportes` | `region` | — | BP | Moderada | 7 |
| D-21 | `/admin-reportes` | `color-contrast` | 1.4.3 | AA | Grave | 4 |
| D-22 | `/admin-reportes` | `select-name` | 4.1.2 | A | Crítica | 2 |
| D-23 | `/admin-reportes` | `label` | 4.1.2 | A | Crítica | 1 |
| D-24 | `/admin-reportes` | `heading-markup` | 1.3.1 | AA | Grave | 1 |
| D-25 | `/admin-reportes` | `landmark-one-main` | — | BP | Moderada | 1 |
| D-26 | `/admin-reportes` | `page-has-heading-one` | — | BP | Moderada | 1 |
| D-27 | `/residente-home` | `color-contrast` | 1.4.3 | AA | Grave | 4 |
| D-28 | `/residente-home` | `select-name` | 4.1.2 | A | Crítica | 1 |
| D-29 | `/residente-home` | `page-has-heading-one` | — | BP | Moderada | 1 |
| D-30 | `/residente-home` | `aria-allowed-role` | — | BP | Menor | 1 |
| D-31 | `/residente-perfil` | `color-contrast` | 1.4.3 | AA | Grave | 6 |
| D-32 | `/residente-historial` | `color-contrast` | 1.4.3 | AA | Grave | 21 |
| D-33 | `/residente-historial` | `scrollable-region-focusable` | 2.1.1 | A | Grave | 1 |
| D-34 | `/residente-documentos` | `color-contrast` | 1.4.3 | AA | Grave | 8 |
| D-35 | `/residente-documentos` | `scrollable-region-focusable` | 2.1.1 | A | Grave | 1 |
| D-36 | `/residente-documentos` | `heading-order` | — | BP | Moderada | 1 |
| D-37 | `/residente-cambiar-contra` | `color-contrast` | 1.4.3 | AA | Grave | 2 |

---

## 6. Análisis de causa raíz

Los 219 hallazgos **no son 219 defectos distintos**. Se reducen a cuatro causas:

| # | Causa raíz | Hallazgos que explica | % del total |
|---|-----------|----------------------:|------------:|
| 1 | Las páginas carecen de estructura semántica: sin `<main>`, sin `<h1>`, contenido fuera de regiones | **128** | 58 % |
| 2 | Una paleta de colores con contraste insuficiente, repetida en toda la interfaz | **74** | 34 % |
| 3 | Controles de formulario sin etiqueta accesible | **17** | 8 % |
| 4 | Contenedores con desplazamiento no alcanzables por teclado | **3** | 1 % |

### Detalle de la causa 2: colores que incumplen el contraste

Los 74 fallos provienen de **12 combinaciones de color**, y solo **cuatro** originan 42:

| Texto | Fondo | Ratio actual | Mínimo exigido | Ocurrencias | Dónde aparece |
|-------|-------|-------------:|---------------:|------------:|---------------|
| `#1e8e3e` | `#e6f4ea` | 3.70 | 4.5 | 17 | Etiquetas de estado «presente» |
| `#888888` | `#ffffff` | 3.54 | 4.5 | 10 | Texto secundario |
| `#999999` | `#ffffff` | 2.84 | 4.5 | 9 | Texto secundario |
| `#7d6693` | `#f3ecf7` | 4.32 | 4.5 | 6 | Etiquetas moradas |
| `#d93025` | `#fce8e6` | 4.05 | 4.5 | 5 | Etiquetas de error |
| `#777777` | `#ffffff` | 4.47 | 4.5 | 4 | Texto secundario |
| `#6c757d` | `#f8f9fa` | 4.44 | 4.5 | 3 | Texto atenuado |
| `#ffffff` | `#28a745` | 3.13 | 4.5 | 2 | Botón verde |
| `#27ae60` | `#d4efdf` | 2.35 | 4.5 | 2 | Etiqueta verde clara |
| `#a569bd` | `#fcf8ff` | 3.73 | 4.5 | 2 | Etiqueta lila |
| `#ffffff` | `#5dade2` | 2.45 | 4.5 | 1 | Botón azul |
| `#aaaaaa` | `#fcfbfe` | 2.25 | 4.5 | 1 | Texto deshabilitado |

> Varios están **muy cerca** del umbral (4.44 y 4.47 frente a 4.5): basta oscurecer el
> texto un par de tonos para cumplir, sin alterar la identidad visual.

---

## 7. Plan de acción propuesto

| Prioridad | Acción | Hallazgos que resuelve | Esfuerzo | Criterio |
|:---------:|--------|----------------------:|----------|----------|
| **1** | Añadir `<main>` y un `<h1>` por página; envolver el contenido en regiones semánticas (`<header>`, `<nav>`, `<main>`) | 128 | Bajo | Buenas prácticas |
| **2** | Asociar `<label>` a cada campo, y `aria-label` a los desplegables y al botón sin texto | 17 | Bajo | **4.1.2 (A)** |
| **3** | Ajustar los 12 pares de color a un contraste ≥ 4.5:1 | 74 | Medio | **1.4.3 (AA)** |
| **4** | Añadir `tabindex="0"` a los contenedores con desplazamiento | 3 | Bajo | **2.1.1 (A)** |
| **5** | Convertir en `<h_>` real el texto que aparenta ser encabezado y corregir el orden jerárquico | 2 | Bajo | **1.3.1 (AA)** |

**Impacto acumulado:** las acciones 1 y 2, ambas de esfuerzo bajo, resuelven **145 de
los 219 hallazgos (66 %)**, incluidos **todos los críticos**.

---

## 8. Conclusiones

1. **La cobertura de la evaluación es total:** las 13 pantallas de la aplicación fueron
   analizadas.
2. **Dos pantallas ya son conformes** (`/login` y `/admin-cambiar-contrasena`), lo que
   demuestra que la base tecnológica permite cumplir la norma.
3. **Los incumplimientos WCAG son 89**, no 219: más de la mitad de los hallazgos son
   buenas prácticas recomendadas por la herramienta, no exigencias de la norma.
4. **Ningún hallazgo requiere rediseñar la aplicación.** Se trata de estructura
   semántica, etiquetas y ajustes de color: correcciones localizadas y de bajo riesgo.
5. **El 66 % de los hallazgos se resuelve con dos acciones de esfuerzo bajo**, que además
   eliminan la totalidad de los hallazgos críticos.

---

## Anexo. Evidencia

Los informes originales exportados por axe DevTools se conservan en
`docs/axe devtools/`:

| Carpeta | Contenido |
|---------|-----------|
| `docs/axe devtools/` | Pantallas de administración |
| `docs/axe devtools/residente/` | Pantallas del residente |
| `docs/axe devtools/Faltantes/` | Perfil y cambio de contraseña del residente |

> Las pantallas `/login` y `/admin-cambiar-contrasena` no tienen archivo de evidencia
> porque la herramienta no reportó ningún hallazgo en ellas.
