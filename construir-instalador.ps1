# ============================================================================
#  CASA KETTELER - Construir el instalador (.exe)
# ============================================================================
#  Reune todo lo que hay que llevar a la residencia y lo empaqueta en UN SOLO
#  archivo ejecutable con asistente grafico.
#
#  Esto se ejecuta en la computadora de DESARROLLO, no en la residencia.
#
#  USO:
#      .\construir-instalador.ps1                  # paquete normal (~80 MB)
#      .\construir-instalador.ps1 -ConModelos      # + modelos de IA (~340 MB)
#      .\construir-instalador.ps1 -SinInternet     # + librerias Python (~2.5 GB)
#
#  El resultado queda en  instalador\salida\CasaKetteler-Instalador-<version>.exe
#  (la version sale de CasaKetteler.iss; hoy: 1.1)
# ============================================================================

[CmdletBinding()]
param(
    # Incluye los modelos de reconocimiento facial ya descargados (~260 MB).
    # Ahorra esa descarga en la PC de destino.
    [switch] $ConModelos,

    # Incluye TODAS las librerias de Python (~2 GB) para instalar sin Internet.
    # Usalo si la red de destino es lenta o restringida.
    [switch] $SinInternet,

    # NO incluye el instalador de Python (ahorra 25 MB, pero en destino hara
    # falta instalarlo a mano si no lo tiene).
    [switch] $SinPython,

    # Incluye tambien el instalador de Java (170 MB). Solo hace falta si la PC de
    # destino no tiene Java y su red no permite descargarlo.
    [switch] $ConJava,

    # Compila el backend antes de empaquetar.
    [switch] $Compilar
)

$ErrorActionPreference = "Stop"

$raiz       = $PSScriptRoot
$instalador = Join-Path $raiz "instalador"
$carga      = Join-Path $instalador "carga"
$salida     = Join-Path $instalador "salida"

function Paso { param([string] $T) Write-Host "`n  == $T" -ForegroundColor Cyan }
function Ok   { param([string] $T) Write-Host "     [OK] $T" -ForegroundColor Green }
function Nota { param([string] $T) Write-Host "     ->   $T" -ForegroundColor Gray }
function Morir {
    param([string] $T, [string] $Como)
    Write-Host "`n     [ERROR] $T" -ForegroundColor Red
    if ($Como) { Write-Host "             $Como" -ForegroundColor Yellow }
    exit 1
}

Write-Host ""
Write-Host "  ==================================================================" -ForegroundColor Cyan
Write-Host "     CASA KETTELER - Construccion del instalador" -ForegroundColor Cyan
Write-Host "  ==================================================================" -ForegroundColor Cyan

# ---------------------------------------------------------------------------
#  1. Compilador de Inno Setup
# ---------------------------------------------------------------------------
Paso "Buscando el compilador de Inno Setup"

$posibles = @(
    "$env:LOCALAPPDATA\Programs\Inno Setup 6\ISCC.exe",
    "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe",
    "$env:ProgramFiles\Inno Setup 6\ISCC.exe"
)
$iscc = $posibles | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $iscc) {
    Morir "No se encontro ISCC.exe (el compilador de Inno Setup)." `
          "Instalalo con:  winget install --id JRSoftware.InnoSetup --source winget"
}
Ok "Compilador: $iscc"

# ---------------------------------------------------------------------------
#  2. Compilar el backend (opcional)
# ---------------------------------------------------------------------------
if ($Compilar) {
    Paso "Compilando el backend"
    & (Join-Path $raiz "mvnw.cmd") clean package -q
    if ($LASTEXITCODE -ne 0) { Morir "Fallo la compilacion del backend." "Revisa la salida de Maven." }
    Ok "Backend compilado."
}

# ---------------------------------------------------------------------------
#  3. Reunir la carga util
# ---------------------------------------------------------------------------
Paso "Reuniendo los archivos a empaquetar"

if (Test-Path $carga) { Remove-Item $carga -Recurse -Force }
New-Item -ItemType Directory -Path $carga -Force | Out-Null

# --- Backend (JAR) ---
$jar = Get-ChildItem (Join-Path $raiz "target\*.jar") -ErrorAction SilentlyContinue |
       Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
       Select-Object -First 1
if (-not $jar) { Morir "No hay ningun JAR en target\." "Genera el backend con:  .\mvnw.cmd clean package" }
New-Item -ItemType Directory -Path (Join-Path $carga "target") -Force | Out-Null
Copy-Item $jar.FullName (Join-Path $carga "target") -Force
Ok ("Backend: {0} ({1:N1} MB)" -f $jar.Name, ($jar.Length / 1MB))

# --- Interfaz web ---
$frontend = Join-Path $raiz "frontend"
if (-not (Test-Path (Join-Path $frontend "index.html"))) {
    Morir "Falta frontend\index.html." `
          "Genera la interfaz (npx ng build) y copiala con:  .\actualizar-frontend.ps1"
}
Copy-Item $frontend (Join-Path $carga "frontend") -Recurse -Force
$nFront = (Get-ChildItem (Join-Path $carga "frontend") -Recurse -File).Count
Ok "Interfaz web: $nFront archivos."

# --- Reconocimiento facial (sin el entorno virtual: se crea en destino) ---
$py = Join-Path $carga "python_scripts"
New-Item -ItemType Directory -Path $py -Force | Out-Null
Copy-Item (Join-Path $raiz "python_scripts\*.py") $py -Force
Copy-Item (Join-Path $raiz "python_scripts\requirements.txt") $py -Force
Ok "Scripts de reconocimiento facial (sin venv_perfecto)."

# --- Scripts de operacion ---
$scripts = @(
    "iniciar-casa-ketteler.bat", "iniciar-casa-ketteler.ps1",
    "detener-casa-ketteler.bat", "detener-casa-ketteler.ps1",
    "actualizar-frontend.ps1", "ejecutar-backend.ps1",
    "instalar.bat", "instalar.ps1",
    "actualizar-sistema.bat", "actualizar-sistema.ps1"
)
foreach ($s in $scripts) {
    $ruta = Join-Path $raiz $s
    if (-not (Test-Path $ruta)) { Morir "Falta el script $s" "No se puede empaquetar sin el." }
    Copy-Item $ruta $carga -Force
}
Ok "$($scripts.Count) scripts de operacion."

# --- application.properties (vive en src\main\resources, NO en la raiz) ---
$props = Join-Path $raiz "src\main\resources\application.properties"
if (-not (Test-Path $props)) { Morir "Falta src\main\resources\application.properties" "" }
Copy-Item $props $carga -Force
Ok "application.properties"

# --- Documentacion de operacion ---
$docsOrigen = Join-Path $raiz "docs"
$docsDestino = Join-Path $carga "docs"
New-Item -ItemType Directory -Path $docsDestino -Force | Out-Null
foreach ($d in @("OPERACION.md", "DESPLIEGUE.md", "ARQUITECTURA.md", "README.md")) {
    $ruta = Join-Path $docsOrigen $d
    if (Test-Path $ruta) { Copy-Item $ruta $docsDestino -Force }
}
Ok "Documentacion de operacion."

# --- Instalador de Python (siempre, salvo -SinPython) ---
# Se incluye a proposito: winget esta restringido en muchos equipos de
# laboratorio o de dominio, y sin Python no hay reconocimiento facial.
if (-not $SinPython) {
    $versionPy = "3.12.10"
    $carpetaReq = Join-Path $carga "requisitos"
    New-Item -ItemType Directory -Path $carpetaReq -Force | Out-Null

    # Se guarda una copia local para no volver a bajarlo en cada construccion.
    $cachePy = Join-Path $raiz "instalador\cache"
    if (-not (Test-Path $cachePy)) { New-Item -ItemType Directory -Path $cachePy -Force | Out-Null }
    $archivoPy = Join-Path $cachePy "python-$versionPy-amd64.exe"

    if (-not (Test-Path $archivoPy)) {
        Nota "Descargando el instalador de Python $versionPy (25 MB)..."
        try {
            Invoke-WebRequest -Uri "https://www.python.org/ftp/python/$versionPy/python-$versionPy-amd64.exe" `
                              -OutFile "$archivoPy.part" -UseBasicParsing -TimeoutSec 300 -ErrorAction Stop
            Move-Item "$archivoPy.part" $archivoPy -Force
        } catch {
            Morir "No se pudo descargar el instalador de Python." `
                  "Comprueba la conexion, o construye con -SinPython (en destino hara falta instalarlo a mano)."
        }
    }
    Copy-Item $archivoPy $carpetaReq -Force
    Ok ("Instalador de Python {0} incluido ({1:N0} MB)." -f $versionPy, ((Get-Item $archivoPy).Length / 1MB))
}

# --- Instalador de Java (opcional) ---
if ($ConJava) {
    $carpetaReq = Join-Path $carga "requisitos"
    if (-not (Test-Path $carpetaReq)) { New-Item -ItemType Directory -Path $carpetaReq -Force | Out-Null }
    $cacheJava = Join-Path $raiz "instalador\cache"
    if (-not (Test-Path $cacheJava)) { New-Item -ItemType Directory -Path $cacheJava -Force | Out-Null }
    $archivoJava = Join-Path $cacheJava "microsoft-jdk-21-windows-x64.msi"

    if (-not (Test-Path $archivoJava)) {
        Nota "Descargando el instalador de Java 21 (170 MB)..."
        try {
            Invoke-WebRequest -Uri "https://aka.ms/download-jdk/microsoft-jdk-21-windows-x64.msi" `
                              -OutFile "$archivoJava.part" -UseBasicParsing -TimeoutSec 600 -ErrorAction Stop
            Move-Item "$archivoJava.part" $archivoJava -Force
        } catch {
            Morir "No se pudo descargar el instalador de Java." "Comprueba la conexion, o construye sin -ConJava."
        }
    }
    Copy-Item $archivoJava $carpetaReq -Force
    Ok ("Instalador de Java 21 incluido ({0:N0} MB)." -f ((Get-Item $archivoJava).Length / 1MB))
}

# --- Modelos de reconocimiento (opcional) ---
if ($ConModelos) {
    $pesosOrigen = Join-Path $env:USERPROFILE ".deepface\weights"
    if (-not (Test-Path $pesosOrigen)) {
        Morir "No hay modelos en $pesosOrigen" `
              "Arranca el reconocimiento facial una vez para que los descargue, o construye sin -ConModelos."
    }
    $pesosDestino = Join-Path $py ".deepface\weights"
    New-Item -ItemType Directory -Path $pesosDestino -Force | Out-Null
    Get-ChildItem $pesosOrigen -File | Where-Object { $_.Extension -ne ".part" } |
        Copy-Item -Destination $pesosDestino -Force
    $mb = ((Get-ChildItem $pesosDestino -File | Measure-Object -Property Length -Sum).Sum) / 1MB
    Ok ("Modelos de reconocimiento: {0:N0} MB (se ahorra esa descarga en destino)." -f $mb)
}

# --- Librerias de Python para instalar sin Internet (opcional) ---
if ($SinInternet) {
    Paso "Descargando las librerias de Python para uso sin Internet"
    Nota "Son unos 2 GB: la primera vez tarda."

    $venvPy = Join-Path $raiz "python_scripts\venv_perfecto\Scripts\python.exe"
    if (-not (Test-Path $venvPy)) {
        Morir "No hay entorno de Python en este equipo para descargar los paquetes." `
              "Crea el entorno aqui primero, o construye sin -SinInternet."
    }
    $paquetes = Join-Path $carga "paquetes-python"
    New-Item -ItemType Directory -Path $paquetes -Force | Out-Null

    & $venvPy -m pip download -r (Join-Path $py "requirements.txt") `
        --dest $paquetes --extra-index-url "https://download.pytorch.org/whl/cpu"
    if ($LASTEXITCODE -ne 0) {
        Morir "No se pudieron descargar todos los paquetes." "Revisa la conexion y reintenta."
    }
    $mb = ((Get-ChildItem $paquetes -File | Measure-Object -Property Length -Sum).Sum) / 1MB
    Ok ("Paquetes de Python: {0:N0} MB en {1} archivos." -f $mb, (Get-ChildItem $paquetes -File).Count)
}

$pesoCarga = ((Get-ChildItem $carga -Recurse -File | Measure-Object -Property Length -Sum).Sum) / 1MB
Nota ("Carga total sin comprimir: {0:N1} MB" -f $pesoCarga)

# ---------------------------------------------------------------------------
#  4. Compilar el instalador
# ---------------------------------------------------------------------------
Paso "Compilando el instalador"

if (-not (Test-Path $salida)) { New-Item -ItemType Directory -Path $salida -Force | Out-Null }

Push-Location $instalador
try {
    # La salida de ISCC es larga: solo se muestran las lineas relevantes.
    & $iscc "/Qp" "CasaKetteler.iss" 2>&1 | ForEach-Object {
        $l = if ($_ -is [System.Management.Automation.ErrorRecord]) { $_.Exception.Message } else { $_.ToString() }
        if ($l -match "error|warning|Successful|Compil") { Write-Host "     $l" -ForegroundColor DarkGray }
    }
    $codigo = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($codigo -ne 0) { Morir "El compilador de Inno Setup devolvio el codigo $codigo." "Revisa los mensajes de arriba." }

$exe = Get-ChildItem (Join-Path $salida "*.exe") | Sort-Object LastWriteTime | Select-Object -Last 1
if (-not $exe) { Morir "No se genero el .exe" "" }

Ok ("Instalador generado: {0:N1} MB" -f ($exe.Length / 1MB))

# ---------------------------------------------------------------------------
#  Resumen
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "  ==================================================================" -ForegroundColor Cyan
Write-Host "     LISTO" -ForegroundColor Green
Write-Host "  ==================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "     $($exe.FullName)" -ForegroundColor White
Write-Host ""
Write-Host "     Llevate SOLO ese archivo a la computadora de la residencia." -ForegroundColor White
Write-Host "     Alli: clic derecho -> Ejecutar como administrador." -ForegroundColor White
Write-Host ""
if (-not $SinInternet) {
    Write-Host "     Recuerda: en destino hara falta Internet para las librerias" -ForegroundColor Yellow
    Write-Host "     de Python (~2 GB). Usa -SinInternet para incluirlas." -ForegroundColor Yellow
}
Write-Host "     MySQL Server 8 se instala aparte, antes de este instalador." -ForegroundColor Yellow
Write-Host ""
