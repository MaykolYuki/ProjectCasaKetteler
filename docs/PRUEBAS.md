# 8. PRUEBAS

## 8.1 Pruebas Unitarias

### Objetivo

Verificar de forma automatizada las **reglas de negocio críticas** del sistema —
aquellas cuyo mal funcionamiento comprometería la asistencia, la seguridad o la
integridad de los datos— de manera independiente y repetible.

La estrategia priorizó **cubrir las reglas que sostienen el sistema** antes que
alcanzar un porcentaje de cobertura. Las pruebas del backend simulan la base de datos
con *mocks*, por lo que se ejecutan en segundos y en cualquier equipo, sin depender de
MySQL.

### Herramientas y entorno

| Componente | Backend | Frontend |
|------------|---------|----------|
| Lenguaje | Java 21 | TypeScript |
| Framework de pruebas | JUnit 5 | Vitest |
| Simulación (*mocks*) | Mockito | — |
| Aserciones | AssertJ | expect (Vitest) |
| Comando de ejecución | `.\mvnw.cmd test` | `npx ng test --watch=false` |

### Resumen de resultados

| Módulo | N.º de pruebas | Resultado |
|--------|:--------------:|:---------:|
| **Backend** | | |
| Registro de asistencia | 7 | ✅ Aprobado |
| Inicio de sesión y seguridad | 6 | ✅ Aprobado |
| Nombres de documentos | 5 | ✅ Aprobado |
| Respaldo de archivos | 3 | ✅ Aprobado |
| Carga del contexto | 1 | ✅ Aprobado |
| **Frontend** | | |
| Fecha de reportes | 4 | ✅ Aprobado |
| Almacenamiento de sesión | 7 | ✅ Aprobado |
| Motivo de asistencia | 5 | ✅ Aprobado |
| **TOTAL** | **38** | **✅ 38 / 38** |

---

### Casos de prueba — Backend

#### Registro de asistencia (`BusinessAttendanceTest`)

| ID | Caso de prueba | Resultado esperado | Estado |
|----|----------------|--------------------|:------:|
| PU-01 | El residente estaba fuera y marca | Registra ENTRADA y queda "presente" | ✅ |
| PU-02 | El residente estaba dentro y marca | Registra SALIDA y queda "ausente" | ✅ |
| PU-03 | Vuelve a marcar antes de 5 minutos | Se rechaza (evita marcas duplicadas) | ✅ |
| PU-04 | Dos eventos seguidos del mismo tipo | Se marca como anomalía | ✅ |
| PU-05 | Secuencia correcta (entrada→salida) | No se marca como anomalía | ✅ |
| PU-06 | El rostro no coincide | Guarda INTENTO_FALLIDO, no registra asistencia | ✅ |
| PU-07 | Celular fuera de la red de la residencia | Rechaza la marca | ✅ |

#### Inicio de sesión y seguridad (`BusinessUserLoginTest`)

| ID | Caso de prueba | Resultado esperado | Estado |
|----|----------------|--------------------|:------:|
| PU-08 | Contraseña correcta | Entrega token y limpia los intentos fallidos | ✅ |
| PU-09 | Inicio de sesión en otro dispositivo | Invalida las sesiones anteriores (sesión única) | ✅ |
| PU-10 | Contraseña incorrecta | No entrega token y suma el intento | ✅ |
| PU-11 | Quinto intento fallido | Bloquea la cuenta temporalmente | ✅ |
| PU-12 | Cuenta bloqueada con contraseña correcta | No permite entrar | ✅ |
| PU-13 | Cuenta desactivada | No permite iniciar sesión | ✅ |

#### Nombres de documentos (`DocumentNameHelperTest`)

| ID | Caso de prueba | Resultado esperado | Estado |
|----|----------------|--------------------|:------:|
| PU-14 | Nombre normal | Genera `TIPO_Nombre_Apellido_fecha` | ✅ |
| PU-15 | Nombre con tildes y espacios | Los limpia para que sea válido en disco | ✅ |
| PU-16 | Ya existe un archivo igual | Agrega sufijo `_2` en vez de sobrescribir | ✅ |
| PU-17 | Archivo sin extensión | No deja un punto suelto al final | ✅ |
| PU-18 | Faltan datos del residente | Usa un marcador, no falla | ✅ |

#### Respaldo de archivos (`BackupServiceTest`)

| ID | Caso de prueba | Resultado esperado | Estado |
|----|----------------|--------------------|:------:|
| PU-19 | Comprimir fotos y documentos | Genera un .zip conservando la estructura de carpetas | ✅ |
| PU-20 | No existe la carpeta de archivos | No rompe el sistema | ✅ |
| PU-21 | Respaldos vencidos | Elimina los que superan el tiempo de conservación | ✅ |

#### Carga del contexto (`ProjectcasakettelerApplicationTests`)

| ID | Caso de prueba | Resultado esperado | Estado |
|----|----------------|--------------------|:------:|
| PU-22 | Arranque de la aplicación | El contexto de Spring se carga sin errores | ✅ |

---

### Casos de prueba — Frontend

#### Fecha de reportes (`fecha-local.spec.ts`)

| ID | Caso de prueba | Resultado esperado | Estado |
|----|----------------|--------------------|:------:|
| PU-23 | Cálculo de fecha a las 22:30 | No adelanta al día siguiente (usa hora local) | ✅ |
| PU-24 | Cálculo de fecha en la madrugada | No retrocede al día anterior | ✅ |
| PU-25 | Formato de fecha | Rellena con ceros el mes y el día | ✅ |
| PU-26 | Fecha del dispositivo | Coincide con la fecha local | ✅ |

> *Esta prueba corresponde a la regresión del error "los reportes salían vacíos en el
> rango diario", causado por el uso de UTC en lugar de la hora local.*

#### Almacenamiento de sesión (`storage.util.spec.ts`)

| ID | Caso de prueba | Resultado esperado | Estado |
|----|----------------|--------------------|:------:|
| PU-27 | Guardar sesión de residente | Persistente (sobrevive al cierre de la app) | ✅ |
| PU-28 | Guardar sesión de administrador | Solo por sesión (se pierde al cerrar el navegador) | ✅ |
| PU-29 | Lectura del token | Lo obtiene sin importar en qué almacén quedó | ✅ |
| PU-30 | Cambio de tipo de sesión | No deja copias viejas del token | ✅ |
| PU-31 | Cierre de sesión | Limpia ambos almacenes | ✅ |
| PU-32 | Respuesta exitosa del backend | La reconoce correctamente | ✅ |
| PU-33 | Mensaje del backend | Toma el primero o usa el de respaldo | ✅ |

#### Motivo de asistencia (`motivo.spec.ts`)

| ID | Caso de prueba | Resultado esperado | Estado |
|----|----------------|--------------------|:------:|
| PU-34 | Entrada sin motivo | Asume "Retorno a la residencia" | ✅ |
| PU-35 | Entrada con motivo elegido | Respeta el motivo indicado | ✅ |
| PU-36 | Salida con motivo | Usa el motivo elegido | ✅ |
| PU-37 | Opción "Otros" | Usa el texto libre, sin espacios sobrantes | ✅ |
| PU-38 | Salida sin motivo | No inventa ninguno (el formulario lo exige) | ✅ |

#### Perfil entre sesiones (`perfil-entre-sesiones.spec.ts`)

Pruebas de **regresión** de un fallo detectado en pruebas de uso: el perfil se guarda
en una señal en memoria, y al cerrar sesión se limpiaban el token y el usuario pero no
esa señal. Como la pantalla de perfil solo consulta al servidor cuando la señal está
vacía, quien entraba después veía los datos del anterior: tras usar el sistema como
administrador y entrar luego como residente, el perfil del residente mostraba el
**correo del administrador**.

| ID | Caso de prueba | Resultado esperado | Estado |
|----|----------------|--------------------|:------:|
| PU-39 | Cerrar sesión | Descarta el perfil que quedaba en memoria | ✅ |
| PU-40 | Iniciar sesión | Descarta el perfil de la sesión anterior | ✅ |
| PU-41 | Residente tras un administrador | Ve su propio correo, no el del administrador | ✅ |
| PU-42 | Sin limpiar la memoria | Se mostrarían datos ajenos (comprueba que PU-41 detecta el fallo) | ✅ |

---

### Cómo ejecutar las pruebas

```powershell
# Backend — 22 pruebas
.\mvnw.cmd test

# Solo un módulo
.\mvnw.cmd test -Dtest=BusinessAttendanceTest

# Frontend — 20 pruebas (en la carpeta del proyecto Angular)
npx ng test --watch=false
```

> **Nota:** las pruebas del backend usan *mocks* y no requieren base de datos, con la
> única excepción de la carga del contexto (PU-22), que sí necesita MySQL encendido.

> ⚠️ En el frontend hay que usar **`ng test`**, no `vitest` directamente. El constructor
> de Angular genera antes el entorno de pruebas (`init-testbed`, el DOM y el compilador);
> al invocar `vitest` por su cuenta ese entorno no existe y las pruebas fallan por
> `localStorage is not defined` o `TestBed.initTestEnvironment()`, aunque el código esté bien.

### Conclusión

Las 42 pruebas unitarias se ejecutan satisfactoriamente y cubren las reglas centrales
del sistema: la lógica de asistencia (alternancia entrada/salida, ventana de 5 minutos,
detección de anomalías, validación por rostro y por red), la seguridad del acceso
(bloqueo por intentos, sesión única, cuentas inactivas) y la integridad de los datos
(nombres de documentos y respaldos). Esto brinda una red de seguridad ante cambios
futuros: cualquier modificación que rompa una de estas reglas será detectada de inmediato.

---

## 8.2 Pruebas de Carga

### Objetivo

Verificar que el sistema atiende, de manera estable y con tiempos de respuesta
aceptables, una cantidad de usuarios concurrentes **muy superior** a la del uso real
esperado, sin degradarse ni producir errores.

### Herramienta y entorno

| Elemento | Detalle |
|----------|---------|
| Herramienta | Apache JMeter 5.6.3 (ejecución sin interfaz, por consola) |
| Servidor | Backend Spring Boot real + MySQL 8, escuchando en `http://localhost:8001` |
| Pool de conexiones | HikariCP, máximo 10 conexiones a la base de datos |
| Máquina | AMD Ryzen 5 3550H (4 núcleos / 8 hilos), 15.7 GB RAM, Windows 11 |
| Autenticación | Un único inicio de sesión al arranque (usuario administrador de prueba); el token JWT se reutiliza en todas las peticiones |

> **Nota metodológica:** por tratarse de un entorno de desarrollo, el generador de carga
> (JMeter), el servidor y la base de datos se ejecutaron en **la misma máquina**,
> compitiendo por CPU. Los valores obtenidos son, por tanto, **conservadores**: en un
> servidor dedicado el rendimiento sería mayor.

### Alcance

Se midieron las tres operaciones de **lectura** más frecuentes del panel de
administración, todas respaldadas por consultas a la base de datos:

- `GET /casaketteler/indexuser` — listado de residentes.
- `GET /casaketteler/attendance/kpi` — indicadores agregados de asistencia.
- `GET /casaketteler/attendance/filter` — historial de asistencia paginado.

Quedaron **fuera de alcance** por diseño:

- El **reconocimiento facial** (`register`): depende del servicio Python y de imágenes
  reales; su tiempo (3–5 s) es intrínseco al algoritmo, no a la concurrencia. Su
  comportamiento se documenta en el requisito RNF-05.
- El **inicio de sesión**: cuenta con un limitador de 15 intentos por IP cada 15 minutos
  (protección contra fuerza bruta), por lo que no es un objetivo válido de saturación.

### Escenario de carga

| Parámetro | Valor |
|-----------|-------|
| Usuarios concurrentes | 20 |
| Rampa de subida | 10 s |
| Duración sostenida | 60 s |
| Operación de cada usuario | Ciclo continuo: listar residentes → KPIs → historial |

### Resultados

**Global**

| Métrica | Resultado |
|---------|-----------|
| Peticiones totales | 3 655 |
| Rendimiento (*throughput*) | **59 peticiones/s** |
| Latencia promedio | 302 ms |
| Percentil 95 (p95) | 661 ms |
| Percentil 99 (p99) | 994 ms |
| Latencia máxima | 3 192 ms |
| **Errores** | **0 (0.00 %)** |

**Por operación**

| Operación | Peticiones | Latencia promedio | Errores |
|-----------|:----------:|:-----------------:|:-------:|
| Listar residentes (`indexuser`) | 1 227 | 475 ms | 0 |
| KPIs de asistencia (`kpi`) | 1 215 | 210 ms | 0 |
| Historial filtrado (`filter`) | 1 212 | 221 ms | 0 |

### Interpretación

Con 20 usuarios concurrentes —un orden de magnitud por encima del uso real, donde la
residencia opera con 2 o 3 administradores— el sistema respondió **sin un solo error** y
con una latencia promedio de **0.3 s**; el 95 % de las peticiones se resolvió en menos de
0.7 s. La operación más costosa es el listado de residentes (475 ms), por incluir los
datos de los 17 residentes. Se concluye que el sistema **soporta holgadamente la carga
esperada** con tiempos de respuesta cómodos para una interfaz web.

---

## 8.3 Pruebas de Estrés

### Objetivo

Someter al sistema a una carga **creciente y muy por encima de lo normal** para
identificar su punto de saturación (capacidad máxima), su comportamiento ante la
sobrecarga y su límite de ruptura.

### Escenario

Se repitió el mismo perfil de operaciones de la prueba de carga, incrementando de forma
escalonada el número de usuarios concurrentes: **50 → 100 → 200 → 400 → 800**, con 30 s
sostenidos por cada nivel.

### Resultados

| Usuarios | Rendimiento (req/s) | Latencia prom. | p95 | Latencia máx. | Errores |
|:--------:|:-------------------:|:--------------:|:---:|:-------------:|:-------:|
| 20 *(carga)* | 59 | 302 ms | 661 ms | 3.2 s | 0.00 % |
| 50 | 71 | 614 ms | 1 102 ms | 2.3 s | 0.00 % |
| 100 | 71 | 1 246 ms | 2 106 ms | 4.6 s | 0.00 % |
| 200 | 75 | 2 334 ms | 3 412 ms | 5.2 s | 0.00 % |
| 400 | 73 | 4 538 ms | 8 444 ms | 10.0 s | 0.00 % |
| 800 | 70 | 9 568 ms | 15 814 ms | 20.8 s | 0.00 % |

### Interpretación

Los resultados dibujan una **curva de saturación de libro de texto**:

1. **Techo de rendimiento (~70–75 peticiones/s).** A partir de 50 usuarios el
   *throughput* deja de crecer y se estabiliza en torno a 70 peticiones por segundo:
   es la **capacidad máxima** de la máquina de prueba. Añadir más usuarios ya no procesa
   más trabajo por segundo.

2. **Punto de saturación (“rodilla”): ~50 usuarios concurrentes.** Por debajo, sumar
   usuarios aumenta el rendimiento; por encima, solo aumenta la latencia. Esto es
   coherente con la **Ley de Little** (latencia ≈ concurrencia ÷ rendimiento): con 200
   usuarios y 75 req/s la latencia teórica es ≈ 2.7 s, muy cercana a los 2.3 s medidos.

3. **Degradación elegante, sin ruptura.** El hallazgo más importante: **incluso con 800
   usuarios concurrentes el sistema no produjo ni un solo error**. Ante la sobrecarga
   **encola** las peticiones (crece la espera) en lugar de rechazarlas o caerse. La
   latencia máxima observada (20.8 s) se mantuvo por debajo del tiempo de espera
   configurado (30 s), por lo que ninguna petición se perdió.

4. **Cuello de botella.** El límite lo impone la CPU de la máquina de prueba —compartida
   entre generador de carga, servidor y base de datos— junto con el pool de 10 conexiones
   a la base de datos. No se trata de un defecto del software, sino de la capacidad del
   hardware de prueba.

### Conclusión y recomendaciones

El sistema es **robusto**: mantiene 0 % de error desde el uso real hasta 800 usuarios
concurrentes (más de 250 veces la demanda esperada), degradándose de forma controlada.
Para el caso de uso de una residencia universitaria —decenas de residentes y un puñado de
administradores— existe un **margen de capacidad enorme**.

De cara a un despliegue en producción o a un crecimiento futuro, se recomienda:

- Ejecutar el servicio en un **servidor dedicado** (sin compartir CPU con el cliente),
  lo que elevaría el techo de rendimiento observado.
- Si se previera una concurrencia sostenida alta, **aumentar el tamaño del pool de
  conexiones** de HikariCP y ajustar los hilos de Tomcat.

Para el alcance actual del proyecto, sin embargo, **ninguna de estas medidas es necesaria**:
el rendimiento medido excede ampliamente los requisitos.
