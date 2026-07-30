# ============================================================================
#  CASA KETTELER - Preparar el paquete de entrega
# ============================================================================
#  Reune en una sola carpeta (y en un .zip) todo lo que hay que llevarse:
#  el instalador del sistema, la app de los residentes y los manuales.
#
#  Antes de usar esto hay que haber generado el instalador:
#      .\construir-instalador.ps1
#
#  USO:
#      .\preparar-entrega.ps1
#      .\preparar-entrega.ps1 -SinZip     # deja solo la carpeta, sin comprimir
#
#  El resultado queda en  entrega\
# ============================================================================

[CmdletBinding()]
param(
    # No genera el .zip: deja solo la carpeta lista para copiar a una USB.
    [switch] $SinZip
)

$ErrorActionPreference = "Stop"

$raiz    = $PSScriptRoot
$version = "1.0"
$entrega = Join-Path $raiz "entrega"
$kit     = Join-Path $entrega "CasaKetteler-$version"

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
Write-Host "     CASA KETTELER - Paquete de entrega" -ForegroundColor Cyan
Write-Host "  ==================================================================" -ForegroundColor Cyan

# ---------------------------------------------------------------------------
#  1. Carpeta limpia
# ---------------------------------------------------------------------------
Paso "Preparando la carpeta"

if (Test-Path $kit) { Remove-Item -LiteralPath $kit -Recurse -Force }
New-Item -ItemType Directory -Path $kit -Force | Out-Null
Ok "entrega\CasaKetteler-$version\"

# ---------------------------------------------------------------------------
#  2. Instalador del sistema
# ---------------------------------------------------------------------------
Paso "Copiando el instalador del sistema"

$exe = Get-ChildItem (Join-Path $raiz "instalador\salida\*.exe") -ErrorAction SilentlyContinue |
       Sort-Object LastWriteTime | Select-Object -Last 1
if (-not $exe) {
    Morir "No hay ningun instalador en instalador\salida\" `
          "Generalo primero con:  .\construir-instalador.ps1"
}
Copy-Item $exe.FullName $kit -Force
$mbExe = $exe.Length / 1MB
Ok ("{0} ({1:N1} MB)" -f $exe.Name, $mbExe)

# Aviso si el instalador NO trae las librerias de Python incluidas.
if ($mbExe -lt 500) {
    Nota "Este instalador NO incluye las librerias de Python: en destino hara"
    Nota "falta Internet (~2 GB). Para incluirlas:"
    Nota "   .\construir-instalador.ps1 -SinInternet   y repite este script."
}

# ---------------------------------------------------------------------------
#  3. App de los residentes (APK)
# ---------------------------------------------------------------------------
Paso "Copiando la app de los residentes"

$posiblesApk = @(
    "C:\Users\yerry\Documents\Ingenieria de Software\II\Front Ketteler\front-ketteler\android\app\build\outputs\apk\release\app-release.apk",
    "C:\Users\yerry\Documents\Ingenieria de Software\II\CasaKetteler-Pagina\CasaKetteler.apk"
)
$apk = $posiblesApk | Where-Object { Test-Path $_ } | Select-Object -First 1
if ($apk) {
    $carpetaApk = Join-Path $kit "app-residentes"
    New-Item -ItemType Directory -Path $carpetaApk -Force | Out-Null
    Copy-Item $apk (Join-Path $carpetaApk "CasaKetteler.apk") -Force
    Ok ("CasaKetteler.apk ({0:N1} MB)" -f ((Get-Item $apk).Length / 1MB))
} else {
    Nota "No se encontro el APK: el kit ira sin la app de los residentes."
}

# ---------------------------------------------------------------------------
#  4. Manuales
# ---------------------------------------------------------------------------
Paso "Copiando los manuales"

$carpetaDocs = Join-Path $kit "manuales"
New-Item -ItemType Directory -Path $carpetaDocs -Force | Out-Null

# Solo los documentos utiles en la residencia. Los del informe academico
# (pruebas, riesgos, trazabilidad, requisitos) no van aqui.
$manuales = @("OPERACION.md", "DESPLIEGUE.md", "ARQUITECTURA.md")
$copiados = 0
foreach ($m in $manuales) {
    $ruta = Join-Path $raiz "docs\$m"
    if (Test-Path $ruta) { Copy-Item $ruta $carpetaDocs -Force; $copiados++ }
}
Ok "$copiados manuales (operacion, despliegue, arquitectura)."

# ---------------------------------------------------------------------------
#  5. Instrucciones
# ---------------------------------------------------------------------------
Paso "Escribiendo las instrucciones"

$leeme = @(
    "CASA KETTELER - Sistema de registro de asistencia",
    "Version $version",
    "=================================================",
    "",
    "",
    "QUE HAY EN ESTA CARPETA",
    "-----------------------",
    "",
    "  $($exe.Name)",
    "      El sistema completo. Se instala en la computadora que quedara",
    "      operando en la residencia (el servidor).",
    "",
    "  app-residentes\CasaKetteler.apk",
    "      La aplicacion para los celulares de los residentes.",
    "",
    "  manuales\",
    "      OPERACION.md    -> uso diario (para la administradora)",
    "      DESPLIEGUE.md   -> instalacion paso a paso (tecnico)",
    "      ARQUITECTURA.md -> como funciona por dentro (tecnico)",
    "",
    "",
    "ORDEN DE INSTALACION",
    "--------------------",
    "",
    "  PASO 1. Instalar MySQL Server 8",
    "",
    "     Es lo unico que va aparte, porque su asistente obliga a definir",
    "     la contrasena del usuario 'root'.",
    "",
    "     Descarga:  https://dev.mysql.com/downloads/installer/",
    "     ANOTA ESA CONTRASENA: se pedira en el paso siguiente.",
    "",
    "",
    "  PASO 2. Ejecutar $($exe.Name)",
    "",
    "     Clic derecho  ->  Ejecutar como administrador",
    "",
    "     El asistente copia el sistema y prepara la computadora: base de",
    "     datos, librerias de reconocimiento facial y arranque automatico",
    "     al encender la PC.",
    "",
    "     Tarda entre 20 y 45 minutos. Si se corta, vuelve a ejecutarlo:",
    "     continua donde quedo, no empieza de nuevo.",
    "",
    "",
    "  PASO 3. Comprobar que funciona",
    "",
    "     Abrir en el navegador:  http://localhost:8001",
    "",
    "",
    "  PASO 4. Registrar la red Wi-Fi de la residencia",
    "",
    "     IMPORTANTE: sin este dato NINGUN residente puede marcar",
    "     asistencia. Ver 'manuales\DESPLIEGUE.md', apartado 5.1.",
    "",
    "",
    "  PASO 5. Repartir la app a los residentes",
    "",
    "     Pasarles el APK, o mejor, el enlace de la pagina de descarga.",
    "     Las instrucciones de instalacion estan en esa pagina.",
    "",
    "",
    "REQUISITOS DE LA COMPUTADORA",
    "----------------------------",
    "",
    "  * Windows 10 o 11 (64 bits)",
    "  * Permisos de administrador",
    "  * Al menos 6 GB libres en disco",
    "  * Conexion a Internet la primera vez (para las librerias)",
    "  * IP fija reservada en el router",
    "",
    "  Si la PC se restaura al reiniciar (Deep Freeze y similares), la",
    "  instalacion desaparecera al apagarla: solo servira para demostrar.",
    "",
    "",
    "SI ALGO SALE MAL",
    "----------------",
    "",
    "  Todo queda registrado en la carpeta 'logs' de la instalacion",
    "  (por defecto C:\CasaKetteler\logs).",
    "",
    "  Para revisar el estado sin cambiar nada:",
    "     C:\CasaKetteler\instalar.ps1 -SoloVerificar",
    "",
    "",
    "Generado el $(Get-Date -Format 'dd/MM/yyyy')."
)
Set-Content -Path (Join-Path $kit "LEEME-PRIMERO.txt") -Value $leeme -Encoding ASCII
Ok "LEEME-PRIMERO.txt"

# ---------------------------------------------------------------------------
#  6. Comprimir
# ---------------------------------------------------------------------------
$pesoKit = ((Get-ChildItem $kit -Recurse -File | Measure-Object -Property Length -Sum).Sum) / 1MB

if ($SinZip) {
    Nota "Se omite el .zip (-SinZip)."
} elseif ($pesoKit -gt 1800) {
    # Compress-Archive de Windows PowerShell 5.1 no pasa de 2 GB.
    Nota ("La carpeta pesa {0:N0} MB: demasiado para Compress-Archive (limite 2 GB)." -f $pesoKit)
    Nota "Copia la carpeta directamente a la USB, o comprimela con 7-Zip."
} else {
    Paso "Comprimiendo"
    $zip = Join-Path $entrega "CasaKetteler-$version.zip"
    if (Test-Path $zip) { Remove-Item -LiteralPath $zip -Force }

    # No se usa Compress-Archive a proposito: en Windows PowerShell 5.1 escribe
    # los separadores con barra INVERTIDA, contra la especificacion ZIP. En
    # Windows abre bien, pero en Mac, Linux o Android aparecerian archivos con
    # "\" en el nombre en vez de carpetas. Aqui se construye a mano con barras
    # normales, de modo que el paquete se pueda abrir en cualquier parte.
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem

    $nivel = [System.IO.Compression.CompressionLevel]::Fastest   # el .exe ya viene comprimido
    $flujo = [System.IO.File]::Open($zip, [System.IO.FileMode]::Create)
    try {
        $archivo = New-Object System.IO.Compression.ZipArchive($flujo, [System.IO.Compression.ZipArchiveMode]::Create)
        try {
            $baseLargo = (Split-Path $kit -Parent).Length + 1
            foreach ($f in (Get-ChildItem $kit -Recurse -File)) {
                $relativa = $f.FullName.Substring($baseLargo).Replace("\", "/")
                [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archivo, $f.FullName, $relativa, $nivel)
            }
        } finally { $archivo.Dispose() }
    } finally { $flujo.Dispose() }

    Ok ("{0} ({1:N1} MB)" -f (Split-Path $zip -Leaf), ((Get-Item $zip).Length / 1MB))
}

# ---------------------------------------------------------------------------
#  Resumen
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "  ==================================================================" -ForegroundColor Cyan
Write-Host "     LISTO" -ForegroundColor Green
Write-Host "  ==================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "     Carpeta:  $kit" -ForegroundColor White
if (-not $SinZip -and $pesoKit -le 1800) {
    Write-Host "     Zip:      $(Join-Path $entrega "CasaKetteler-$version.zip")" -ForegroundColor White
}
Write-Host ""
Write-Host "     Contenido:" -ForegroundColor Gray
Get-ChildItem $kit -Recurse -File | ForEach-Object {
    $rel = $_.FullName.Substring($kit.Length + 1)
    Write-Host ("       {0,-46} {1,7:N1} MB" -f $rel, ($_.Length / 1MB)) -ForegroundColor Gray
}
Write-Host ""
Write-Host "     Recuerda instalar MySQL Server 8 antes del instalador." -ForegroundColor Yellow
Write-Host ""
