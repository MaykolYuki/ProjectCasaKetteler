# Despliegue en la computadora de la residencia

Cómo instalar el sistema en la PC que quedará operando en la residencia.

> **Idea clave:** a esa computadora se lleva el programa **ya compilado**, no el
> código fuente. No hace falta instalar Git, Node ni Maven allá.

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
| Los 4 scripts `.bat` / `.ps1` | raíz | Encender, apagar, actualizar |
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
