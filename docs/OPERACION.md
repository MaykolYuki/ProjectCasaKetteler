# Manual de operación

Guía para el día a día del sistema. **No necesitas saber programación.**

---

## Encender y apagar

| Acción | Qué hacer |
|--------|-----------|
| **Encender** | Doble clic en `iniciar-casa-ketteler.bat` |
| **Apagar** | Doble clic en `detener-casa-ketteler.bat` |

Normalmente **no tendrás que hacer nada**: el sistema arranca solo cuando se
enciende la computadora.

Al encender aparece una ventana que muestra el estado:

```
   CASA KETTELER - Estado del sistema
   ===================================
   Backend principal      ACTIVO
   Reconocimiento facial  ACTIVO

   El sistema esta listo para usarse.
```

- **ACTIVO** en ambos = todo bien.
- El reconocimiento facial tarda **unos 30 segundos** en encender (carga los modelos).
- Puedes **cerrar esa ventana**: el sistema sigue funcionando. Solo sirve para mirar.

> Si el sistema arranca automáticamente al encender la PC, para apagarlo hay que
> hacer **clic derecho → Ejecutar como administrador** sobre `detener-casa-ketteler.bat`.

---

## Entrar al sistema

| Quién | Cómo |
|-------|------|
| **Administradora** | Navegador → `http://localhost:8001` |
| **Desde otra PC de la residencia** | `http://<IP-del-servidor>:8001` |
| **Residentes** | App instalada en su celular |

La administradora **debe escribir su contraseña cada vez**. Es intencional: la
computadora es compartida y la sesión se cierra al cerrar el navegador.

Los residentes **no** vuelven a iniciar sesión: su celular es personal y la app
recuerda la sesión.

---

## Respaldos

El sistema se respalda **solo**:

| Qué | Cuándo |
|-----|--------|
| Base de datos (todo el historial) | Todos los días a las 2:00 a. m. |
| Fotos y documentos | Los domingos a las 2:30 a. m. |

Se guardan en la carpeta `backups` y se conservan **los últimos 14 días**.

**Respaldo inmediato:** en el panel de administración, botón **"Respaldar ahora"**.
Úsalo **antes de cualquier actualización**.

> ⚠️ **Muy importante:** los respaldos están en el **mismo disco** que el sistema.
> Si ese disco falla, se pierde todo junto. **Copia la carpeta `backups` a una USB
> o a la nube cada cierto tiempo.** Es la única protección real ante una avería.

---

## Problemas comunes

### Un residente no puede marcar asistencia

Revisa en este orden:

1. **¿Está conectado al Wi-Fi de la residencia?** El sistema exige que la marca se
   haga desde la red oficial. Es la causa más frecuente.
2. **¿El sistema está encendido?** Mira la ventana de estado, o abre
   `http://localhost:8001` desde la PC.
3. **¿Aparece "Rostro no reconocido"?** Que se ponga de frente, con buena luz y sin
   gorra ni lentes oscuros. Si sigue fallando, quizá haya que actualizar sus fotos
   de referencia desde *Gestión de usuarios → Editar → Subir fotos*.

### "El servidor no responde" o la app queda cargando

El sistema está apagado o la computadora se reinició. Ejecuta
`iniciar-casa-ketteler.bat` y espera un minuto.

### El reconocimiento facial aparece DETENIDO

1. Ejecuta `detener-casa-ketteler.bat` y luego `iniciar-casa-ketteler.bat`.
2. Si sigue igual, abre la carpeta `logs` y revisa `reconocimiento.error.log`.

### La ventana negra se queda congelada

Si alguna vez abres una ventana de consola y parece trabada, es una peculiaridad de
Windows: **al hacer clic dentro, el programa se pausa**. Pulsa cualquier tecla
(por ejemplo `Esc`) y continuará.

> Con el arranque normal esto ya no ocurre: los programas corren sin ventana.

### La administradora olvidó su contraseña

Otra cuenta de administración puede restablecerla desde
*Gestión de usuarios → Restablecer contraseña*. El sistema genera una temporal.

---

## Registros del sistema (logs)

Si algo falla, la carpeta `logs` guarda el detalle:

| Archivo | Contiene |
|---------|----------|
| `backend.log` | Actividad del sistema principal |
| `backend.error.log` | Errores del sistema principal |
| `reconocimiento.log` | Actividad del reconocimiento facial |
| `reconocimiento.error.log` | Errores del reconocimiento facial |

No hace falta entenderlos: basta enviárselos a quien dé soporte técnico.

---

## Tareas de mantenimiento recomendadas

| Cada cuánto | Qué hacer |
|-------------|-----------|
| **Semanal** | Copiar la carpeta `backups` a una USB o a la nube |
| **Mensual** | Revisar que la PC no esté llena de espacio |
| **Mensual** | Reiniciar la computadora y confirmar que el sistema vuelve solo |
| **Cuando cambien residentes** | Dar de alta/baja en *Gestión de usuarios* |

---

## Qué NO hay que hacer

- ❌ **No borrar** las carpetas `storage`, `backups` ni `logs`.
- ❌ **No mover** los archivos del sistema de lugar.
- ❌ **No cambiar la IP** de la computadora: la app de los residentes dejaría de
  encontrarla y habría que reinstalarla en todos los celulares.
- ❌ **No apagar la computadora** en horarios de entrada/salida de residentes.
