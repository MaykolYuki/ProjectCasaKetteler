# ============================================================================
#  CASA KETTELER - Actualizar la interfaz web (Angular) que sirve el backend
# ============================================================================
#  Copia el build de Angular a la carpeta 'frontend', desde donde el backend la
#  publica en http://<ip-del-servidor>:8001
#
#  NO hace falta regenerar el JAR: la carpeta es externa. Basta reiniciar (o ni eso,
#  porque los archivos se leen en cada petición).
#
#  Antes de ejecutarlo, genera el build en el proyecto del front:
#     npx ng build
# ============================================================================

$ErrorActionPreference = "Stop"

# Ruta del proyecto Angular (ajústala si mueves las carpetas)
$proyectoFront = "C:\Users\yerry\Documents\Ingenieria de Software\II\Front Ketteler\front-ketteler"
$origen = Join-Path $proyectoFront "dist\front-ketteler\browser"
$destino = Join-Path $PSScriptRoot "frontend"

if (-not (Test-Path $origen)) {
    Write-Host "ERROR: no se encontro el build de Angular en:" -ForegroundColor Red
    Write-Host "  $origen" -ForegroundColor Red
    Write-Host "Genera primero el build con:  npx ng build" -ForegroundColor Yellow
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
