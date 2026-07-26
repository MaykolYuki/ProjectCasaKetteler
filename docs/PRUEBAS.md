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

---

### Cómo ejecutar las pruebas

```powershell
# Backend — 22 pruebas
.\mvnw.cmd test

# Solo un módulo
.\mvnw.cmd test -Dtest=BusinessAttendanceTest

# Frontend — 16 pruebas (en la carpeta del proyecto Angular)
npx ng test --watch=false
```

> **Nota:** las pruebas del backend usan *mocks* y no requieren base de datos, con la
> única excepción de la carga del contexto (PU-22), que sí necesita MySQL encendido.

### Conclusión

Las 38 pruebas unitarias se ejecutan satisfactoriamente y cubren las reglas centrales
del sistema: la lógica de asistencia (alternancia entrada/salida, ventana de 5 minutos,
detección de anomalías, validación por rostro y por red), la seguridad del acceso
(bloqueo por intentos, sesión única, cuentas inactivas) y la integridad de los datos
(nombres de documentos y respaldos). Esto brinda una red de seguridad ante cambios
futuros: cualquier modificación que rompa una de estas reglas será detectada de inmediato.

---

## 8.2 Pruebas de Carga

*(En elaboración)*

## 8.3 Pruebas de Estrés

*(En elaboración)*
