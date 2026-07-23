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

Write-Host ""
Write-Host "  Iniciando Casa Ketteler..." -ForegroundColor Cyan
Write-Host ""

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
    Start-Process -FilePath $python -ArgumentList "ServidorReconocimiento.py" -WorkingDirectory $carpetaPython -WindowStyle Minimized
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
    Start-Process -FilePath "java" -ArgumentList "-jar", "`"$jar`"" -WorkingDirectory $PSScriptRoot -WindowStyle Minimized
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
        Write-Host "   Si algo sigue DETENIDO tras un minuto, revisa su ventana minimizada." -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "   Actualizado: $(Get-Date -Format 'HH:mm:ss')"
    Write-Host "   (Puedes cerrar esta ventana: los servidores siguen funcionando.)" -ForegroundColor DarkGray

    Start-Sleep -Seconds 5
}
