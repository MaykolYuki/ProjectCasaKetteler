# ============================================================================
#  CASA KETTELER - Actualizar el sistema instalado
# ============================================================================
#  Reemplaza el backend y/o la interfaz web por una version nueva, comprobando
#  que arranca. Si no arranca, DESHACE el cambio solo y deja el sistema como
#  estaba.
#
#  USO (clic derecho -> Ejecutar como administrador, o desde PowerShell):
#      .\actualizar-sistema.ps1 -Desde "D:\version-nueva"
#      .\actualizar-sistema.ps1 -Desde "D:\version-nueva" -Solo frontend
#      .\actualizar-sistema.ps1 -Deshacer        # vuelve a la version anterior
#      .\actualizar-sistema.ps1 -Desde "..." -SinRespaldoBd
#
#  La carpeta de origen debe contener lo que se quiera actualizar:
#      target\*.jar    -> el backend
#      frontend\       -> la interfaz web
#
#  NOTA sobre la base de datos: se guarda un respaldo antes de actualizar, pero
#  al deshacer NO se restaura solo. Restaurarlo borraria lo que los residentes
#  hubieran registrado despues de la actualizacion; esa decision es de una
#  persona, no del script.
# ============================================================================

[CmdletBinding()]
param(
    # Carpeta con la version nueva.
    [string] $Desde,

    # Actualizar solo una parte.
    [ValidateSet('backend', 'frontend')]
    [string] $Solo,

    # Vuelve a la version guardada en el ultimo respaldo.
    [switch] $Deshacer,

    # Omite el respaldo de la base de datos (mas rapido, menos seguro).
    [switch] $SinRespaldoBd,

    # No hace preguntas.
    [switch] $Desatendido
)

$ErrorActionPreference = "Continue"
$env:PYTHONIOENCODING = "utf-8"

$Carpeta      = $PSScriptRoot
$carpetaLogs  = Join-Path $Carpeta "logs"
$carpetaResp  = Join-Path $Carpeta "backups"
$archivoLog   = Join-Path $carpetaLogs "actualizacion.log"
$PUERTO_BACKEND = 8001
$PUERTO_PYTHON  = 5000
$VERSIONES_A_CONSERVAR = 3

if (-not (Test-Path $carpetaLogs)) { New-Item -ItemType Directory -Path $carpetaLogs -Force | Out-Null }

# ---------------------------------------------------------------------------
#  Presentacion
# ---------------------------------------------------------------------------
function Escribir-Log {
    param([string] $Texto, [string] $Nivel = "INFO")
    $linea = "{0} [{1}] {2}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Nivel, $Texto
    try { Add-Content -Path $archivoLog -Value $linea -Encoding UTF8 -ErrorAction SilentlyContinue } catch { }
}
function Paso { param([string] $T) Write-Host "`n  == $T" -ForegroundColor Cyan; Escribir-Log "== $T" }
function Ok   { param([string] $T) Write-Host "     [OK] $T" -ForegroundColor Green; Escribir-Log $T "OK" }
function Nota { param([string] $T) Write-Host "     ->   $T" -ForegroundColor Gray;  Escribir-Log $T }
function Aviso { param([string] $T) Write-Host "     [!]  $T" -ForegroundColor Yellow; Escribir-Log $T "AVISO" }
function Morir {
    param([string] $T, [string] $Como)
    Write-Host "`n     [ERROR] $T" -ForegroundColor Red
    if ($Como) { Write-Host "             $Como" -ForegroundColor Yellow }
    Escribir-Log "$T | $Como" "ERROR"
    if (-not $Desatendido) { Write-Host ""; Read-Host "     Presiona ENTER para cerrar" | Out-Null }
    exit 1
}

# Evita que un clic dentro de la ventana congele el proceso (modo QuickEdit).
try {
    if (-not ("CasaKetteler.ConsolaAct" -as [type])) {
        $firma = @(
            '[DllImport("kernel32.dll", SetLastError = true)]',
            'public static extern IntPtr GetStdHandle(int nStdHandle);',
            '[DllImport("kernel32.dll", SetLastError = true)]',
            'public static extern bool GetConsoleMode(IntPtr h, out uint m);',
            '[DllImport("kernel32.dll", SetLastError = true)]',
            'public static extern bool SetConsoleMode(IntPtr h, uint m);'
        ) -join "`n"
        Add-Type -MemberDefinition $firma -Name "ConsolaAct" -Namespace "CasaKetteler" -ErrorAction Stop | Out-Null
    }
    $api = [CasaKetteler.ConsolaAct]; $h = $api::GetStdHandle(-10); $modo = 0
    if ($api::GetConsoleMode($h, [ref]$modo)) { [void]$api::SetConsoleMode($h, (($modo -band (-bnot 0x0040)) -bor 0x0080)) }
} catch { }

Write-Host ""
Write-Host "  ==================================================================" -ForegroundColor Cyan
Write-Host "     CASA KETTELER - Actualizacion del sistema" -ForegroundColor Cyan
Write-Host "  ==================================================================" -ForegroundColor Cyan
Escribir-Log "########## Inicio ##########"

# ---------------------------------------------------------------------------
#  Utilidades
# ---------------------------------------------------------------------------
function Detener-Sistema {
    foreach ($p in @($PUERTO_BACKEND, $PUERTO_PYTHON)) {
        $ids = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue |
               Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($id in $ids) { try { Stop-Process -Id $id -Force -ErrorAction Stop } catch { } }
    }
    Start-Sleep -Seconds 3
}

function Arrancar-Sistema {
    $lanzador = Join-Path $Carpeta "iniciar-casa-ketteler.bat"
    if (-not (Test-Path $lanzador)) { return $false }
    Start-Process -FilePath $lanzador -WorkingDirectory $Carpeta -WindowStyle Minimized
    return $true
}

# Comprueba de verdad que la version arrancada responde. Devuelve $true/$false.
function Verificar-Sistema {
    param([int] $EsperaMaxima = 120)
    Nota "Esperando a que el sistema responda (hasta $EsperaMaxima s)..."
    $backendVivo = $false
    for ($s = 0; $s -lt $EsperaMaxima; $s += 3) {
        Start-Sleep -Seconds 3
        if (Get-NetTCPConnection -LocalPort $PUERTO_BACKEND -State Listen -ErrorAction SilentlyContinue) {
            $backendVivo = $true; break
        }
    }
    if (-not $backendVivo) {
        Aviso "El backend no llego a escuchar en el puerto $PUERTO_BACKEND."
        return $false
    }
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:$PUERTO_BACKEND/casaketteler/attendance/health" `
                -UseBasicParsing -TimeoutSec 20
        if ($r.StatusCode -eq 200) { Ok "El sistema responde correctamente."; return $true }
        Aviso "Respondio con codigo $($r.StatusCode)."
        return $false
    } catch {
        Aviso "El puerto esta abierto pero no responde: $($_.Exception.Message)"
        return $false
    }
}

function Copiar-Contenido {
    param([string] $Origen, [string] $Destino)
    if (Test-Path $Destino) { Remove-Item $Destino -Recurse -Force }
    New-Item -ItemType Directory -Path $Destino -Force | Out-Null
    Copy-Item (Join-Path $Origen "*") $Destino -Recurse -Force
}

# ---------------------------------------------------------------------------
#  Comprobaciones previas
# ---------------------------------------------------------------------------
$jarActual = Get-ChildItem (Join-Path $Carpeta "target\*.jar") -ErrorAction SilentlyContinue | Select-Object -First 1
$frontActual = Join-Path $Carpeta "frontend"

if (-not $jarActual -and -not (Test-Path $frontActual)) {
    Morir "Aqui no hay una instalacion de Casa Ketteler." `
          "Ejecuta este script desde la carpeta donde esta instalado el sistema."
}

# ===========================================================================
#  DESHACER: volver a la version anterior
# ===========================================================================
if ($Deshacer) {
    Paso "Volviendo a la version anterior"

    $versiones = Get-ChildItem (Join-Path $carpetaResp "version-*") -Directory -ErrorAction SilentlyContinue |
                 Sort-Object Name -Descending
    if (-not $versiones) {
        Morir "No hay ninguna version guardada para volver atras." `
              "Los respaldos se crean al actualizar, en backups\version-<fecha>."
    }

    $ultima = $versiones[0]
    Nota "Se restaurara: $($ultima.Name)"
    if (-not $Desatendido) {
        $r = Read-Host "     Escribe SI para confirmar"
        if ($r -notmatch '^\s*(si|s)\s*$') { Write-Host "     Cancelado."; exit 0 }
    }

    Detener-Sistema
    $jarGuardado = Get-ChildItem (Join-Path $ultima.FullName "target\*.jar") -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($jarGuardado) {
        Get-ChildItem (Join-Path $Carpeta "target\*.jar") -ErrorAction SilentlyContinue | Remove-Item -Force
        Copy-Item $jarGuardado.FullName (Join-Path $Carpeta "target") -Force
        Ok "Backend restaurado: $($jarGuardado.Name)"
    }
    $frontGuardado = Join-Path $ultima.FullName "frontend"
    if (Test-Path $frontGuardado) {
        Copiar-Contenido $frontGuardado $frontActual
        Ok "Interfaz web restaurada."
    }

    if (Arrancar-Sistema) {
        if (Verificar-Sistema) { Ok "El sistema volvio a la version anterior y funciona." }
        else { Aviso "Se restauro la version anterior, pero no responde. Revisa logs\backend.error.log" }
    }
    Write-Host ""
    Write-Host "     La base de datos NO se toco: sigue como estaba." -ForegroundColor Yellow
    Write-Host "     Si hiciera falta volver atras tambien los datos, restaura a mano" -ForegroundColor Yellow
    Write-Host "     el .sql de backups\ correspondiente." -ForegroundColor Yellow
    Write-Host ""
    if (-not $Desatendido) { Read-Host "     Presiona ENTER para cerrar" | Out-Null }
    exit 0
}

# ===========================================================================
#  ACTUALIZAR
# ===========================================================================
if (-not $Desde) {
    Morir "Falta indicar de donde tomar la version nueva." `
          "Ejemplo:  .\actualizar-sistema.ps1 -Desde ""D:\version-nueva"""
}
if (-not (Test-Path $Desde)) { Morir "No existe la carpeta de origen: $Desde" "" }

Paso "Revisando la version nueva"

$jarNuevo = $null
$frontNuevo = $null

if ($Solo -ne 'frontend') {
    $jarNuevo = Get-ChildItem (Join-Path $Desde "target\*.jar") -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
                Select-Object -First 1
    if (-not $jarNuevo) {
        $jarNuevo = Get-ChildItem (Join-Path $Desde "*.jar") -ErrorAction SilentlyContinue | Select-Object -First 1
    }
}
if ($Solo -ne 'backend') {
    foreach ($cand in @((Join-Path $Desde "frontend"), $Desde)) {
        if (Test-Path (Join-Path $cand "index.html")) { $frontNuevo = $cand; break }
    }
}

if (-not $jarNuevo -and -not $frontNuevo) {
    Morir "En esa carpeta no hay nada que instalar." `
          "Debe contener target\*.jar (el backend) o frontend\ con index.html (la interfaz)."
}

# ¿Cambia algo de verdad? Evita parar el sistema para nada.
$cambiaBackend = $false
if ($jarNuevo) {
    if (-not $jarActual) { $cambiaBackend = $true }
    else {
        $h1 = (Get-FileHash $jarNuevo.FullName -Algorithm SHA256).Hash
        $h2 = (Get-FileHash $jarActual.FullName -Algorithm SHA256).Hash
        $cambiaBackend = $h1 -ne $h2
    }
    if ($cambiaBackend) { Ok ("Backend nuevo: {0} ({1:N1} MB)" -f $jarNuevo.Name, ($jarNuevo.Length / 1MB)) }
    else { Nota "El backend es identico al instalado: no se tocara." }
}
if ($frontNuevo) {
    $n = (Get-ChildItem $frontNuevo -Recurse -File).Count
    Ok "Interfaz web nueva: $n archivos."
}

if (-not $cambiaBackend -and -not $frontNuevo) {
    Write-Host ""
    Write-Host "     No hay nada que actualizar." -ForegroundColor Green
    if (-not $Desatendido) { Read-Host "     Presiona ENTER para cerrar" | Out-Null }
    exit 0
}

# ---------------------------------------------------------------------------
#  Respaldo de la version actual
# ---------------------------------------------------------------------------
Paso "Guardando la version actual"

if (-not (Test-Path $carpetaResp)) { New-Item -ItemType Directory -Path $carpetaResp -Force | Out-Null }
$destinoResp = Join-Path $carpetaResp ("version-" + (Get-Date -Format "yyyyMMdd-HHmmss"))
New-Item -ItemType Directory -Path $destinoResp -Force | Out-Null

if ($jarActual) {
    New-Item -ItemType Directory -Path (Join-Path $destinoResp "target") -Force | Out-Null
    Copy-Item $jarActual.FullName (Join-Path $destinoResp "target") -Force
    Ok "Backend actual guardado."
}
if (Test-Path (Join-Path $frontActual "index.html")) {
    Copiar-Contenido $frontActual (Join-Path $destinoResp "frontend")
    Ok "Interfaz web actual guardada."
}
Nota "Copia en backups\$(Split-Path $destinoResp -Leaf)"

# Solo se conservan las ultimas versiones, para no llenar el disco.
$viejas = Get-ChildItem (Join-Path $carpetaResp "version-*") -Directory -ErrorAction SilentlyContinue |
          Sort-Object Name -Descending | Select-Object -Skip $VERSIONES_A_CONSERVAR
foreach ($v in $viejas) { Remove-Item $v.FullName -Recurse -Force -ErrorAction SilentlyContinue }
if ($viejas) { Nota "Se descartaron $(@($viejas).Count) version(es) antiguas." }

# ---------------------------------------------------------------------------
#  Respaldo de la base de datos
# ---------------------------------------------------------------------------
if (-not $SinRespaldoBd -and $cambiaBackend) {
    Paso "Guardando la base de datos"
    # Una version nueva del backend puede cambiar la estructura de las tablas,
    # asi que conviene tener el estado previo antes de tocarla.
    $claves = @{}
    $archivoEnv = Join-Path $Carpeta ".env"
    if (Test-Path $archivoEnv) {
        Get-Content $archivoEnv | ForEach-Object {
            $l = $_.Trim()
            if ($l -and -not $l.StartsWith("#") -and $l.Contains("=")) {
                $i = $l.IndexOf("="); $claves[$l.Substring(0, $i).Trim()] = $l.Substring($i + 1).Trim()
            }
        }
    }
    $dump = Get-ChildItem "C:\Program Files\MySQL\MySQL Server 8*\bin\mysqldump.exe" -ErrorAction SilentlyContinue |
            Select-Object -First 1
    if ($dump -and $claves["DB_PASSWORD"]) {
        $sql = Join-Path $carpetaResp ("bd-antes-de-actualizar-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".sql")
        $nombreBd = if ($claves["DB_NAME"]) { $claves["DB_NAME"] } else { "casaKetteler" }
        & cmd /c "`"$($dump.FullName)`" -uroot -p$($claves['DB_PASSWORD']) $nombreBd > `"$sql`"" 2>$null
        if ((Test-Path $sql) -and (Get-Item $sql).Length -gt 0) {
            Ok ("Base de datos guardada ({0:N1} MB)." -f ((Get-Item $sql).Length / 1MB))
        } else {
            Aviso "No se pudo respaldar la base de datos; se continua igualmente."
        }
    } else {
        Aviso "No se encontro mysqldump o la contraseña: se omite el respaldo de la base."
    }
}

# ---------------------------------------------------------------------------
#  Reemplazo
# ---------------------------------------------------------------------------
Paso "Instalando la version nueva"

# La interfaz web se lee en cada peticion: se puede cambiar sin parar nada.
# El backend si necesita reinicio, porque el JAR esta en uso.
$hayQueParar = $cambiaBackend
if ($hayQueParar) {
    Nota "Deteniendo el sistema..."
    Detener-Sistema
    Ok "Sistema detenido."
}

if ($cambiaBackend) {
    New-Item -ItemType Directory -Path (Join-Path $Carpeta "target") -Force | Out-Null
    Get-ChildItem (Join-Path $Carpeta "target\*.jar") -ErrorAction SilentlyContinue | Remove-Item -Force
    Copy-Item $jarNuevo.FullName (Join-Path $Carpeta "target") -Force
    Ok "Backend reemplazado."
}
if ($frontNuevo) {
    Copiar-Contenido $frontNuevo $frontActual
    # El build de Angular trae dos indices; se sirve el de cliente.
    $csr = Join-Path $frontActual "index.csr.html"
    if (Test-Path $csr) { Copy-Item $csr (Join-Path $frontActual "index.html") -Force }
    Ok "Interfaz web reemplazada."
}

# ---------------------------------------------------------------------------
#  Arranque y verificacion
# ---------------------------------------------------------------------------
Paso "Comprobando que la version nueva funciona"

if (-not (Arrancar-Sistema)) {
    Morir "No se encontro iniciar-casa-ketteler.bat para arrancar el sistema." ""
}

if (Verificar-Sistema) {
    Write-Host ""
    Write-Host "  ==================================================================" -ForegroundColor Cyan
    Write-Host "     ACTUALIZACION COMPLETADA" -ForegroundColor Green
    Write-Host "  ==================================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "     Version anterior guardada en:" -ForegroundColor White
    Write-Host "       backups\$(Split-Path $destinoResp -Leaf)" -ForegroundColor White
    Write-Host ""
    Write-Host "     Si algo va mal mas adelante:  .\actualizar-sistema.ps1 -Deshacer" -ForegroundColor White
    Write-Host ""
    Escribir-Log "########## Actualizacion correcta ##########"
    if (-not $Desatendido) { Read-Host "     Presiona ENTER para cerrar" | Out-Null }
    exit 0
}

# ---------------------------------------------------------------------------
#  No arranco: se deshace solo
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "     La version nueva NO funciona. Deshaciendo el cambio..." -ForegroundColor Red
Escribir-Log "La version nueva no respondio: se deshace" "ERROR"

Detener-Sistema

if ($cambiaBackend -and $jarActual) {
    Get-ChildItem (Join-Path $Carpeta "target\*.jar") -ErrorAction SilentlyContinue | Remove-Item -Force
    Copy-Item (Join-Path $destinoResp "target\$($jarActual.Name)") (Join-Path $Carpeta "target") -Force
    Ok "Backend anterior restaurado."
}
if ($frontNuevo -and (Test-Path (Join-Path $destinoResp "frontend"))) {
    Copiar-Contenido (Join-Path $destinoResp "frontend") $frontActual
    Ok "Interfaz web anterior restaurada."
}

Arrancar-Sistema | Out-Null
if (Verificar-Sistema) {
    Write-Host ""
    Write-Host "     Se volvio a la version anterior y el sistema funciona." -ForegroundColor Yellow
} else {
    Write-Host ""
    Write-Host "     ATENCION: tampoco responde la version anterior." -ForegroundColor Red
    Write-Host "     Revisa logs\backend.error.log" -ForegroundColor Red
}

Write-Host ""
Write-Host "     La actualizacion NO se aplico. Revisa que el JAR nuevo" -ForegroundColor Yellow
Write-Host "     corresponda a esta instalacion y vuelve a intentarlo." -ForegroundColor Yellow
Write-Host "     Detalle en logs\actualizacion.log y logs\backend.error.log" -ForegroundColor Yellow
Write-Host ""
Escribir-Log "########## Revertido ##########"
if (-not $Desatendido) { Read-Host "     Presiona ENTER para cerrar" | Out-Null }
exit 1
