# Despliegue en la computadora de la residencia

Cómo instalar el sistema en la PC que quedará operando en la residencia.

> **Idea clave:** a esa computadora se lleva el programa **ya compilado**, no el
> código fuente. No hace falta instalar Git, Node ni Maven allá.

---

## Camino rápido: un solo archivo

**En tu computadora**, genera el instalador:

```powershell
.\construir-instalador.ps1                  # ~73 MB
.\construir-instalador.ps1 -ConModelos      # ~330 MB, evita bajar los modelos allá
.\construir-instalador.ps1 -SinInternet     # ~2.5 GB, instala sin red en destino
```

Queda en `instalador\salida\CasaKetteler-Instalador-1.0.exe`. **Ese único archivo es
todo lo que llevas** — dentro van el backend, la interfaz web, los scripts de
reconocimiento, la documentación y el instalador.

**En la computadora de la residencia:**

```
clic derecho en  CasaKetteler-Instalador-1.0.exe  ->  Ejecutar como administrador
```

Asistente en español: acepta, elige carpeta (por defecto `C:\CasaKetteler`) y listo.
Copia los archivos, prepara el entorno, crea los accesos en el menú Inicio y deja el
sistema arrancando solo al encender la PC. Trae desinstalador.

> **Por qué `C:\CasaKetteler` y no *Archivos de programa*:** el sistema escribe
> continuamente en `storage\`, `logs\` y `backups\`, y dentro de *Archivos de programa*
> Windows lo bloquearía.

### Alternativa: solo los scripts

Si prefieres copiar los archivos a mano (paso 1 y 3 de esta guía), la preparación del
entorno también se puede lanzar sola:

```
clic derecho en  instalar.bat  ->  Ejecutar como administrador
```

El instalador comprueba los programas base, crea las carpetas, genera el `.env` (con
un `JWT_SECRET` nuevo y la IP de la PC), prepara la base de datos, instala el entorno
de Python, deja los modelos listos, registra el arranque automático y termina
comprobando que el sistema responde.

| Detalle | Comportamiento |
|---------|----------------|
| **Se puede repetir** | Cada paso mira si ya está hecho y se salta lo que no hace falta. |
| **Se reanuda** | Si se corta a mitad (por ejemplo, se cae la conexión durante los ~2 GB de librerías), al volver a ejecutarlo continúa donde quedó: lo ya descargado se reutiliza. |
| **No destruye nada** | Respeta un `.env` existente (solo completa lo que falte, y guarda una copia del anterior) y **no** restaura un respaldo sobre una base de datos con datos. |
| **Deja registro** | Todo queda en `logs\instalacion.log`; el avance, en `logs\instalacion-estado.json`. |
| **Dice qué falta** | Termina con una lista de pendientes. Código de salida `0` = completo, `2` = quedan pendientes. |

Opciones útiles:

```powershell
.\instalar.ps1 -SoloVerificar                    # revisa el estado sin cambiar nada
.\instalar.ps1 -RestaurarRespaldo respaldo.sql   # además restaura datos (si la BD está vacía)
```

**El instalador también deja lista la red:** crea la regla del Firewall de Windows para
el puerto 8001 y comprueba, al final, que el sistema **responde desde la red** y no solo
desde la propia computadora. Sin esa regla el sistema funcionaría en el servidor pero
ningún celular conectaría, y sin causa visible: Windows normalmente pregunta la primera
vez, pero aquí nunca lo hace, porque el sistema arranca como SYSTEM desde la tarea
programada y no hay nadie que responda el aviso.

> ⚠️ Si Windows tiene catalogada la red de la residencia como **pública**, seguirá
> bloqueando aunque exista la regla. El instalador lo detecta y avisa. Se corrige en
> *Configuración → Red e Internet*, cambiando esa red a **privada**.

**Qué sigue necesitando mano humana:**

- Instalar **MySQL Server 8** *antes*, y tener a mano la contraseña de `root` (su
  asistente obliga a definirla, por eso no se automatiza). El instalador lo detecta y
  avisa si falta.
- El **correo** para enviar contraseñas temporales (`MAIL_USERNAME` / `MAIL_PASSWORD`).
- El **SSID/BSSID** de la residencia (paso 5.1): el instalador lo intenta leer y lo
  guarda solo si Windows lo permite y la residencia ya está registrada.
- **Permisos de administrador** en esa PC. Sin ellos el instalador se detiene: los
  necesita para el arranque automático y el servicio de MySQL.

> ⚠️ **Si es una PC de laboratorio o de uso compartido**, comprueba que no se restaure
> al reiniciar (Deep Freeze y similares): en ese caso la instalación desaparece al
> apagar, y solo sirve para demostrar.

El resto de esta guía explica los mismos pasos **a mano**, por si algo falla o hay que
entender qué ocurre por dentro.

---

## 1. Qué preparar antes de ir

Compila todo en tu computadora de desarrollo:

```powershell
# Backend
cd "ruta\a\ProjectCasaKetteler"
.\detener-casa-ketteler.bat      # si estuviera corriendo
.\mvnw.cmd clean package

# Frontend
cd "ruta\al\front-ketteler"
npx ng build
```

Copia a una USB:

| Qué copiar | De dónde | Notas |
|-----------|----------|-------|
| `projectcasaketteler-0.0.1-SNAPSHOT.jar` | `target\` | El backend compilado |
| Contenido de `browser\` | `dist\front-ketteler\` | La interfaz web |
| Carpeta `python_scripts\` | raíz del proyecto | **Sin** `venv_perfecto` (ver paso 4) |
| `.env` y `application.properties` | raíz y `src\main\resources\` | Se ajustan allá |
| Los scripts `.bat` / `.ps1` | raíz | Encender, apagar, actualizar e **instalar** |
| `instalar.bat` + `instalar.ps1` | raíz | El instalador automático (ver arriba) |
| Respaldo `.sql` | `backups\` | Si migras datos existentes |
| `app-release.apk` | `android\app\build\outputs\apk\release\` | Para los residentes |

---

## 2. Instalar los programas base

En la PC de la residencia:

| Programa | Notas |
|----------|-------|
| **JDK 21** | Verifica con `java -version` |
| **MySQL Server 8** | Anota la contraseña de `root` |
| **Python 3.10–3.12** | Marca *"Add Python to PATH"* al instalar |

> **Da a esta PC una IP fija** en el router. Si la IP cambia, la app de los
> residentes deja de conectar y hay que regenerar el APK.

---

## 3. Colocar los archivos

Crea una carpeta, por ejemplo `C:\CasaKetteler`, y deja dentro:

```
C:\CasaKetteler\
├── projectcasaketteler-0.0.1-SNAPSHOT.jar
├── .env
├── frontend\                    ← contenido de dist\front-ketteler\browser\
├── python_scripts\
├── storage\                     ← fotos y documentos
├── backups\                     ← se crea sola
├── iniciar-casa-ketteler.bat
├── detener-casa-ketteler.bat
└── ... (los .ps1 correspondientes)
```

> ⚠️ El backend busca el JAR en `target\`. Si prefieres esa estructura, crea la
> carpeta `target` y pon el JAR ahí, o ajusta la ruta en `iniciar-casa-ketteler.ps1`.

**Importante sobre el índice de la web:** en `frontend\` debe existir `index.html`.
El build de Angular genera además `index.csr.html`; cópialo sobre `index.html`
(el script `actualizar-frontend.ps1` ya lo hace).

---

## 4. Recrear el entorno de Python

**No copies `venv_perfecto` de otra computadora**: guarda rutas absolutas y no funciona.

```powershell
cd C:\CasaKetteler\python_scripts
python -m venv venv_perfecto
.\venv_perfecto\Scripts\activate
pip install -r requirements.txt
```

Descarga unos 2 GB. La primera ejecución baja además los modelos de IA.

---

## 5. Base de datos

```sql
CREATE DATABASE casaKetteler CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Si traes datos de otra instalación:

```powershell
mysql -u root -p casaKetteler < "C:\CasaKetteler\respaldo.sql"
```

### 5.1 Registrar la red Wi-Fi de la residencia (paso manual obligatorio)

La validación de asistencia por **ubicación** compara la red del celular contra el
SSID/BSSID guardados en la tabla `tresidence`. Estos valores **se cargan a mano**: el
sistema **no puede leer automáticamente el BSSID** del punto de acceso (Windows lo
restringe por permisos), así que hay que obtenerlo y escribirlo en la base de datos.

> ⚠️ Si la residencia no tiene la red configurada, **ningún residente podrá marcar
> asistencia** (el sistema responde *"La residencia no tiene una red WiFi configurada"*).

1. Conecta la PC a la red Wi-Fi **oficial** de la residencia.
2. Obtén el SSID y el BSSID:

   ```powershell
   netsh wlan show interfaces
   ```

   Anota **SSID** (nombre de la red) y **BSSID** (MAC del punto de acceso, formato
   `aa:bb:cc:dd:ee:ff`).
3. Guárdalos en la residencia correspondiente:

   ```sql
   UPDATE tresidence
      SET wifiSsid  = 'NOMBRE_DE_LA_RED',
          wifiBssid = 'aa:bb:cc:dd:ee:ff'
    WHERE idResidence = '<id-de-la-residencia>';
   ```

**Cómo valida el sistema** (importante para decidir qué llenar):

- Si `wifiBssid` **está lleno**, se valida **solo por BSSID** (más estricto: identifica
  el punto de acceso exacto). El SSID se ignora.
- Si `wifiBssid` está **vacío**, se usa `wifiSsid` como respaldo (valida por nombre de red).
- Si **ambos** están vacíos, la asistencia queda bloqueada.

> **Recomendación:** si la residencia tiene **un solo** punto de acceso, usa el **BSSID**
> (más seguro). Si tiene **varios** (repetidores/malla, cada uno con distinto BSSID),
> deja el BSSID vacío y configura solo el **SSID**, común a todos.
>
> **Cuándo repetir este paso:** si se **cambia el router o el punto de acceso**, su BSSID
> cambia y hay que actualizar este valor, o los residentes dejarán de poder marcar.

---

## 6. Ajustar la configuración a esta PC

**`.env`** — lo que cambia sí o sí:

```properties
DB_USERNAME=root
DB_PASSWORD=<la de esta PC>

APP_STORAGE_PATH=C:/CasaKetteler/storage
APP_TEMP_PATH=C:/CasaKetteler/temp

JWT_SECRET=<una cadena larga y aleatoria, distinta a la de desarrollo>

CORS_ALLOWED_ORIGINS=http://localhost:8001,capacitor://localhost,http://<IP-DE-ESTA-PC>:8001
```

**`application.properties`** — verifica la ruta de `mysqldump` (para los respaldos):

```properties
app.backup.mysqldump=C:/Program Files/MySQL/MySQL Server 8.0/bin/mysqldump.exe
```

> Este archivo **no viaja en git**, por eso hay que copiarlo a mano.

---

## 7. Encender y comprobar

Doble clic en **`iniciar-casa-ketteler.bat`**. Aparece una ventana de estado:

```
   CASA KETTELER - Estado del sistema
   ===================================
   Backend principal      ACTIVO
   Reconocimiento facial  ACTIVO
```

Comprobaciones:

| Qué | Cómo |
|-----|------|
| La web abre | `http://localhost:8001` |
| Desde otra PC de la red | `http://<IP-DE-ESTA-PC>:8001` |
| El login funciona | Entrar con la cuenta de administración |

---

## 8. Que arranque solo al encender la PC

Esto es lo que hace que el sistema esté disponible sin depender de nadie.

1. Abrir **Programador de tareas** → *Crear tarea…* (no la básica)
2. **General:** nombre `Casa Ketteler`; marcar *Ejecutar con los privilegios más altos*
3. **Desencadenadores:** nuevo → *Al iniciar el equipo*
4. **Acciones:** nuevo → *Iniciar un programa* → seleccionar `iniciar-casa-ketteler.bat`
5. **Configuración:** marcar *Si la tarea falla, reiniciarla cada 1 minuto*

Verifica también que **MySQL sea un servicio automático**:
`services.msc` → MySQL80 → *Tipo de inicio: Automático*.

**Prueba real:** reinicia la computadora y confirma que el sistema vuelve solo.

> ⚠️ Si la tarea corre con privilegios elevados, para apagar el sistema hay que
> ejecutar `detener-casa-ketteler.bat` **como administrador** (clic derecho).

---

## Generar el APK para repartir

El APK ya está firmado y listo en:

```
android\app\build\outputs\apk\release\app-release.apk
```

Para regenerarlo tras un cambio:

```powershell
cd "ruta\al\front-ketteler"
npm run build:android
npx cap sync android
cd android
.\gradlew.bat assembleRelease
```

### Cómo lo instalan los residentes

Se les pasa el archivo por WhatsApp, Drive o USB. Al abrirlo, Android pedirá
permitir *"instalar aplicaciones de origen desconocido"* para la app desde la que
lo abren. **No hace falta el modo desarrollador.**

> 🔑 **La llave de firma es irreemplazable.** Los archivos
> `android/casaketteler-release.keystore` y `android/keystore.properties` deben estar
> respaldados en un lugar seguro. Android exige que **toda actualización esté firmada
> con la misma llave**: si se pierde, no se podrá actualizar la app nunca más.

---

## Actualizar una instalación existente

```powershell
# 1. Apagar (como administrador si arranca automático)
.\detener-casa-ketteler.bat

# 2. Reemplazar lo que cambió
#    - JAR nuevo      -> si cambió el backend
#    - carpeta frontend -> si cambió la interfaz

# 3. Encender
.\iniciar-casa-ketteler.bat
```

**La interfaz web se puede actualizar sola**, sin tocar el JAR: basta reemplazar el
contenido de `frontend\`.

Si cambió la app móvil, hay que repartir el APK nuevo a los residentes.

> 💡 Antes de actualizar, pulsa **"Respaldar ahora"** en el panel del administrador.
