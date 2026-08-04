# Instalación del entorno de desarrollo

Cómo dejar el proyecto funcionando en **otra computadora** para poder programar.
Si lo que quieres es instalarlo en la residencia, ve a [DESPLIEGUE.md](DESPLIEGUE.md).

> Tiempo estimado: **1 a 2 horas**, casi todo esperando descargas.

---

## 1. Programas que hay que instalar

| Programa | Versión | Para qué | Verificar con |
|----------|---------|----------|---------------|
| **JDK** | 21 | Backend | `java -version` |
| **MySQL Server** | 8.x | Base de datos | `mysql --version` |
| **Python** | 3.10 – 3.12 | Reconocimiento facial | `python --version` |
| **Node.js** | 20 o superior | Interfaz y app móvil | `node -v` |
| **Git** | cualquiera | Control de versiones | `git --version` |
| **Android Studio** | reciente | Solo si vas a generar el APK | — |

> ⚠️ **Python 3.13 aún no es compatible** con las versiones de TensorFlow que usa el
> reconocimiento facial. Usa 3.10, 3.11 o 3.12.

---

## 2. Descargar el código

```powershell
git clone <url-del-repositorio-backend>
cd ProjectCasaKetteler
```

El **frontend está en un repositorio aparte**; clónalo donde prefieras.

---

## 3. Base de datos

Crea la base vacía (las tablas se generan solas al arrancar el backend):

```sql
CREATE DATABASE casaKetteler CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> Hibernate está en modo `update`: crea y actualiza las tablas automáticamente.
> **No borra datos.**

Si te pasan un respaldo `.sql`, restáuralo así:

```powershell
mysql -u root -p casaKetteler < "ruta\al\respaldo.sql"
```

---

## 4. Configurar el backend

Ambos archivos están en `.gitignore` (contienen contraseñas), por eso hay que
crearlos a partir de las plantillas:

```powershell
copy .env.example .env
copy application.properties.example src\main\resources\application.properties
```

Ahora **edita `.env`** y ajusta:

| Variable | Qué poner |
|----------|-----------|
| `DB_USERNAME` / `DB_PASSWORD` | Tus credenciales de MySQL |
| `APP_STORAGE_PATH` | Ruta **absoluta** a la carpeta `storage` de este proyecto |
| `APP_TEMP_PATH` | Ruta **absoluta** a la carpeta `temp` |
| `JWT_SECRET` | Una cadena larga y aleatoria (genera la tuya) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Correo Gmail y su *contraseña de aplicación* |
| `CORS_ALLOWED_ORIGINS` | Agrega la IP de esta PC (`ipconfig`) para probar con celular |

> Usa **barras normales** `/` en las rutas, incluso en Windows.

Para generar un `JWT_SECRET`:

```powershell
-join ((48..57)+(65..90)+(97..122) | Get-Random -Count 64 | % {[char]$_})
```

---

## 5. Entorno de Python (reconocimiento facial)

Esta es la parte más pesada: descarga TensorFlow y los modelos de IA.

```powershell
cd python_scripts
python -m venv venv_perfecto
.\venv_perfecto\Scripts\activate
pip install -r requirements.txt
```

> 📦 Son unos **2 GB** entre librerías y modelos. La primera ejecución descarga
> además los modelos de DeepFace (ArcFace y el detector), lo que tarda unos minutos.

> ⚠️ **El entorno virtual NO se puede copiar de otra computadora.** Guarda rutas
> absolutas internamente. Siempre créalo con los comandos de arriba.

Para probar que quedó bien:

```powershell
$env:STORAGE_PATH = "C:/ruta/a/ProjectCasaKetteler/storage"
python ServidorReconocimiento.py
```

Debe mostrar `=== Modelos cargados correctamente ===` y quedarse escuchando.

---

## 6. Frontend

```powershell
cd "ruta\al\front-ketteler"
npm install
```

Luego edita `src/environments/environment.ts` y pon la IP de tu PC:

```typescript
export const environment = {
  production: false,
  apiBaseUrl: 'http://192.168.1.100:8001/casaketteler',  // ← tu IP
};
```

> Se usa la **IP de la red y no `localhost`** porque el celular necesita alcanzar
> tu computadora. Averígualas con `ipconfig`. Esa misma IP debe estar en
> `CORS_ALLOWED_ORIGINS` del `.env`.

---

## 7. Levantar todo para desarrollar

Necesitas **tres cosas corriendo** a la vez:

```powershell
# 1) Reconocimiento facial
cd python_scripts
.\venv_perfecto\Scripts\activate
$env:STORAGE_PATH = "C:/ruta/a/ProjectCasaKetteler/storage"
python ServidorReconocimiento.py

# 2) Backend (otra terminal)
.\mvnw.cmd spring-boot:run

# 3) Frontend (otra terminal, en el proyecto Angular)
npx ng serve
```

| Dónde entrar | Dirección |
|--------------|-----------|
| Interfaz web en desarrollo | http://localhost:4200 |
| API del backend | http://localhost:8001/casaketteler |

> 💡 **Atajo:** si no vas a modificar el frontend, `iniciar-casa-ketteler.bat`
> levanta el backend y el reconocimiento facial de una sola vez (requiere haber
> generado el JAR con `.\mvnw.cmd clean package`).

---

## 8. Ejecutar las pruebas

```powershell
# Backend (22 pruebas)
.\mvnw.cmd test

# Solo un grupo
.\mvnw.cmd test -Dtest=BusinessAttendanceTest

# Frontend (20 pruebas)
npx ng test --watch=false
```

> La prueba `contextLoads` necesita **MySQL encendido**; las demás no.

**Pruebas de carga y estrés (opcional).** Requieren [Apache JMeter](https://jmeter.apache.org/)
y el backend en ejecución. El plan y las instrucciones están en
[docs/pruebas-carga/](pruebas-carga/):

```powershell
jmeter -n -t docs/pruebas-carga/casaketteler-load.jmx -l carga.jtl -Jthreads=20 -Jrampup=10 -Jduration=60
```

> Para probar en local la validación de asistencia **por red**, la residencia debe tener
> el SSID/BSSID cargado en `tresidence` (ver [DESPLIEGUE.md 5.1](DESPLIEGUE.md)).

---

## 9. Generar el APK

```powershell
npm run ship:android     # compila e instala en un celular conectado por USB
```

Para un APK **firmado** que puedas repartir, ver
[DESPLIEGUE.md](DESPLIEGUE.md#generar-el-apk-para-repartir).

---

## Problemas frecuentes

| Síntoma | Causa y solución |
|---------|------------------|
| `Failed to determine a suitable driver class` | Falta `application.properties` (cópialo de la plantilla) |
| El backend arranca pero falla al crear `jwtHelper` | No se cargó el `.env`. Usa `.\ejecutar-backend.ps1` |
| `mvn package` falla al copiar el JAR | El sistema está corriendo. Ejecuta `detener-casa-ketteler.bat` |
| El celular no conecta | La app apunta a `localhost` en vez de la IP, o falta esa IP en `CORS_ALLOWED_ORIGINS` |
| La consola de Python se congela | Es el *modo edición rápida* de Windows: pulsa una tecla. Ver [OPERACION](OPERACION.md) |
| `No module named deepface` | No activaste el entorno virtual (`.\venv_perfecto\Scripts\activate`) |
