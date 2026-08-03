# ============================================================================
#  CASA KETTELER - Inicio del sistema completo
# ============================================================================
#  Arranca los DOS servidores que necesita el sistema y muestra su estado:
#    1. Reconocimiento facial (Python)  -> puerto 5000
#    2. Backend principal (Java)        -> puerto 8001
#
#  USO: doble clic en "iniciar-casa-ketteler.bat" (no hace falta saber comandos).
#  Cada servidor abre su propia ventana minimizada; esta ventana solo monitorea.
#
#  Para detener todo: usa "detener-casa-ketteler.bat".
# ============================================================================

$ErrorActionPreference = "Continue"

function Test-Puerto([int]$puerto) {
    return $null -ne (Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue)
}

# Evita que un clic dentro de esta ventana la congele (modo QuickEdit de Windows).
# Los servidores seguirian corriendo igual, pero el estado se quedaria detenido y
# parece que el sistema fallo.
function Desactivar-PausaPorClic {
    try {
        if (-not ("CasaKetteler.Consola" -as [type])) {
            $firma = @(
                '[DllImport("kernel32.dll", SetLastError = true)]',
                'public static extern IntPtr GetStdHandle(int nStdHandle);',
                '[DllImport("kernel32.dll", SetLastError = true)]',
                'public static extern bool GetConsoleMode(IntPtr h, out uint m);',
                '[DllImport("kernel32.dll", SetLastError = true)]',
                'public static extern bool SetConsoleMode(IntPtr h, uint m);'
            ) -join "`n"
            Add-Type -MemberDefinition $firma -Name "Consola" -Namespace "CasaKetteler" -ErrorAction Stop | Out-Null
        }
        $api = [CasaKetteler.Consola]
        $entrada = $api::GetStdHandle(-10)
        $modo = 0
        if (-not $api::GetConsoleMode($entrada, [ref]$modo)) { return }
        [void]$api::SetConsoleMode($entrada, (($modo -band (-bnot 0x0040)) -bor 0x0080))
    } catch { }
}

Desactivar-PausaPorClic

# --- 1. Cargar las variables del .env (contraseñas, rutas, JWT, etc.) ---
$envFile = Join-Path $PSScriptRoot ".env"
if (-not (Test-Path $envFile)) {
    Write-Host "ERROR: no se encontro el archivo .env junto a este script." -ForegroundColor Red
    Read-Host "Presiona ENTER para cerrar"
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $idx = $line.IndexOf("=")
        $nombre = $line.Substring(0, $idx).Trim()
        $valor = $line.Substring($idx + 1).Trim()
        Set-Item -Path "env:$nombre" -Value $valor
    }
}

# El servidor Python espera STORAGE_PATH; en el .env la variable se llama APP_STORAGE_PATH.
$env:STORAGE_PATH = $env:APP_STORAGE_PATH

# La salida del servidor Python va a un archivo de registro, y en ese caso Windows
# usa cp1252 en vez de UTF-8. DeepFace escribe emojis en sus mensajes, que no
# existen en cp1252: el print falla con UnicodeEncodeError y tumba el servidor.
# Se fuerza UTF-8 aqui por si el .env es de una instalacion anterior.
if (-not $env:PYTHONIOENCODING) { $env:PYTHONIOENCODING = "utf-8" }

Write-Host ""
Write-Host "  Iniciando Casa Ketteler..." -ForegroundColor Cyan
Write-Host ""

# Los servidores se ejecutan SIN ventana de consola y su salida va a archivos de log.
# Motivo: si un servidor corre en una consola visible y alguien hace clic dentro (o
# selecciona texto), el "modo QuickEdit" de Windows CONGELA el proceso hasta pulsar
# una tecla. Sin consola, eso no puede pasar; y los logs quedan para diagnosticar.
$logs = Join-Path $PSScriptRoot "logs"
if (-not (Test-Path $logs)) { New-Item -ItemType Directory -Path $logs | Out-Null }

# --- 2. Servidor de reconocimiento facial (Python) ---
if (Test-Puerto 5000) {
    Write-Host "  - Reconocimiento facial: ya estaba en ejecucion." -ForegroundColor Yellow
} else {
    $python = Join-Path $PSScriptRoot "python_scripts\venv_perfecto\Scripts\python.exe"
    if (-not (Test-Path $python)) {
        Write-Host "  ERROR: no se encontro el entorno de Python (venv_perfecto)." -ForegroundColor Red
        Read-Host "Presiona ENTER para cerrar"
        exit 1
    }
    $carpetaPython = Join-Path $PSScriptRoot "python_scripts"
    Start-Process -FilePath $python -ArgumentList "ServidorReconocimiento.py" -WorkingDirectory $carpetaPython -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logs "reconocimiento.log") -RedirectStandardError (Join-Path $logs "reconocimiento.error.log")
    Write-Host "  - Reconocimiento facial: iniciando (carga los modelos, puede tardar ~30s)."
}

# --- 3. Backend principal (Java) ---
if (Test-Puerto 8001) {
    Write-Host "  - Backend: ya estaba en ejecucion." -ForegroundColor Yellow
} else {
    $jar = Join-Path $PSScriptRoot "target\projectcasaketteler-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path $jar)) {
        Write-Host "  ERROR: falta el archivo del backend (JAR)." -ForegroundColor Red
        Write-Host "  Genera con:  .\mvnw.cmd clean package" -ForegroundColor Red
        Read-Host "Presiona ENTER para cerrar"
        exit 1
    }
    # Se limita la memoria del backend a proposito.
    #
    # Sin -Xmx, Java se reserva hasta la CUARTA PARTE de la RAM del equipo: en una
    # computadora de 8 GB serian 2 GB solo para el backend, compitiendo con el
    # reconocimiento facial, que necesita cerca de 1 GB con los modelos cargados.
    # Con 768 MB va sobrado para una residencia (en reposo usa unos 330 MB) y deja
    # sitio al resto.
    $memoriaBackend = "-Xmx768m"
    Start-Process -FilePath "java" -ArgumentList $memoriaBackend, "-jar", "`"$jar`"" -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logs "backend.log") -RedirectStandardError (Join-Path $logs "backend.error.log")
    Write-Host "  - Backend: iniciando."
}

Start-Sleep -Seconds 3

# --- 4. Monitor de estado (se refresca solo) ---
while ($true) {
    $py = Test-Puerto 5000
    $java = Test-Puerto 8001

    Clear-Host
    Write-Host ""
    Write-Host "   CASA KETTELER - Estado del sistema" -ForegroundColor Cyan
    Write-Host "   ==========================================="
    Write-Host ""

    Write-Host "   Backend principal      " -NoNewline
    if ($java) { Write-Host "ACTIVO" -ForegroundColor Green } else { Write-Host "DETENIDO" -ForegroundColor Red }

    Write-Host "   Reconocimiento facial  " -NoNewline
    if ($py) { Write-Host "ACTIVO" -ForegroundColor Green } else { Write-Host "DETENIDO" -ForegroundColor Red }

    Write-Host ""
    if ($java -and $py) {
        Write-Host "   El sistema esta listo para usarse." -ForegroundColor Green
    } else {
        Write-Host "   Si algo sigue DETENIDO tras un minuto, revisa los archivos de la" -ForegroundColor Yellow
        Write-Host "   carpeta 'logs' para ver que ocurrio." -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "   Actualizado: $(Get-Date -Format 'HH:mm:ss')"
    Write-Host "   (Puedes cerrar esta ventana: los servidores siguen funcionando.)" -ForegroundColor DarkGray

    Start-Sleep -Seconds 5
}
