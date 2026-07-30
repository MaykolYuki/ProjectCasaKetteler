# ============================================================================
#  CASA KETTELER - Actualizar la interfaz web (Angular) que sirve el backend
# ============================================================================
#  Copia el build de Angular a la carpeta 'frontend', desde donde el backend la
#  publica en http://<ip-del-servidor>:8001
#
#  NO hace falta regenerar el JAR: la carpeta es externa. Basta reiniciar (o ni eso,
#  porque los archivos se leen en cada petición).
#
#  COMPILA SOLO antes de copiar, con la configuración "web": esa usa una URL
#  relativa (/casaketteler) en vez de una IP fija, porque el backend sirve esta
#  interfaz desde el mismo origen. Así funciona en cualquier red sin recompilar.
#  (La app Android es distinta: usa la configuración "production", con la IP
#  absoluta, porque se carga desde capacitor://localhost.)
#
#  Uso:
#     .\actualizar-frontend.ps1              # compila y copia
#     .\actualizar-frontend.ps1 -SinCompilar # solo copia lo que ya haya en dist
# ============================================================================

param(
    # Omite la compilación y copia el build existente. Útil si acabas de compilar.
    [switch] $SinCompilar
)

$ErrorActionPreference = "Stop"

# Ruta del proyecto Angular (ajústala si mueves las carpetas)
$proyectoFront = "C:\Users\yerry\Documents\Ingenieria de Software\II\Front Ketteler\front-ketteler"
$origen = Join-Path $proyectoFront "dist\front-ketteler\browser"
$destino = Join-Path $PSScriptRoot "frontend"

if (-not $SinCompilar) {
    Write-Host ""
    Write-Host "  Compilando la interfaz web (configuración 'web')..." -ForegroundColor Cyan
    Push-Location $proyectoFront
    try {
        & npm run build:web
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  ERROR: fallo la compilacion de Angular." -ForegroundColor Red
            Read-Host "Presiona ENTER para cerrar"
            exit 1
        }
    } finally { Pop-Location }
}

if (-not (Test-Path $origen)) {
    Write-Host "ERROR: no se encontro el build de Angular en:" -ForegroundColor Red
    Write-Host "  $origen" -ForegroundColor Red
    Write-Host "Genera el build con:  npm run build:web" -ForegroundColor Yellow
    Read-Host "Presiona ENTER para cerrar"
    exit 1
}

Write-Host ""
Write-Host "  Actualizando la interfaz web..." -ForegroundColor Cyan

if (Test-Path $destino) { Remove-Item $destino -Recurse -Force }
New-Item -ItemType Directory -Path $destino | Out-Null
Copy-Item (Join-Path $origen "*") $destino -Recurse -Force

# El build trae dos indices: el prerenderizado (SSR) y el de cliente (CSR).
# Aquí se sirve como app de cliente, así que se usa el CSR.
$csr = Join-Path $destino "index.csr.html"
if (Test-Path $csr) {
    Copy-Item $csr (Join-Path $destino "index.html") -Force
    Write-Host "  index.csr.html -> index.html" -ForegroundColor DarkGray
}

Write-Host "  Listo. La interfaz esta disponible en http://localhost:8001" -ForegroundColor Green
Write-Host ""
Start-Sleep -Seconds 2
