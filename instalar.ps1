# ============================================================================
#  CASA KETTELER - Instalador del sistema
# ============================================================================
#  Deja la computadora de la residencia lista para operar: comprueba los
#  programas base, crea las carpetas, configura el .env, prepara la base de
#  datos, instala el entorno de Python, descarga los modelos de reconocimiento
#  y registra el arranque automatico.
#
#  USO (clic derecho -> Ejecutar como administrador):
#      .\instalar.bat
#
#  O desde PowerShell como administrador:
#      .\instalar.ps1
#      .\instalar.ps1 -SoloVerificar              # no cambia nada, solo revisa
#      .\instalar.ps1 -RestaurarRespaldo respaldo.sql
#
#  SE PUEDE VOLVER A EJECUTAR SIN MIEDO: cada paso comprueba si ya esta hecho y
#  se salta lo que no hace falta repetir. Si algo falla a mitad (por ejemplo, se
#  corta la descarga), basta ejecutarlo de nuevo y continua donde quedo.
# ============================================================================

[CmdletBinding()]
param(
    # Carpeta donde queda instalado el sistema (por defecto, la de este script).
    [string] $Carpeta = $PSScriptRoot,

    # Respaldo .sql a restaurar (solo si la base de datos esta vacia).
    [string] $RestaurarRespaldo,

    # Revisa el estado sin modificar nada.
    [switch] $SoloVerificar,

    # No hace preguntas: usa valores por defecto y omite lo que necesite respuesta.
    [switch] $Desatendido
)

# "Continue" a proposito: al capturar la salida de programas externos (java,
# python, mysql, pip) con 2>&1, PowerShell 5.1 envuelve cada linea de error en un
# ErrorRecord. Con "Stop" eso aborta el script aunque el programa haya funcionado
# bien. Los fallos de verdad se detectan por el codigo de salida y se lanzan a
# mano con "throw" dentro de cada paso.
$ErrorActionPreference = "Continue"

# ---------------------------------------------------------------------------
#  Constantes
# ---------------------------------------------------------------------------
$JAVA_MINIMO      = 21
$PYTHON_SOPORTADO = @("3.10", "3.11", "3.12")
$VERSION_PYTHON   = "3.12.10"   # la que se instala si falta (descarga de python.org)
$PUERTO_BACKEND   = 8001
$PUERTO_PYTHON    = 5000
$NOMBRE_BD        = "casaKetteler"
$TAREA_PROGRAMADA = "Casa Ketteler"
$ESPACIO_MINIMO_GB = 6
$INDICE_TORCH     = "https://download.pytorch.org/whl/cpu"

$carpetaLogs   = Join-Path $Carpeta "logs"
$archivoLog    = Join-Path $carpetaLogs "instalacion.log"
$archivoEstado = Join-Path $carpetaLogs "instalacion-estado.json"
$cachePip      = Join-Path $carpetaLogs "pip-cache"

$inicio = Get-Date
$script:Advertencias = @()
$script:Pendientes   = @()

# ---------------------------------------------------------------------------
#  Registro y presentacion
# ---------------------------------------------------------------------------

function Escribir-Log {
    param([string] $Texto, [string] $Nivel = "INFO")
    $linea = "{0} [{1}] {2}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Nivel, $Texto
    try { Add-Content -Path $archivoLog -Value $linea -Encoding UTF8 -ErrorAction SilentlyContinue } catch { }
}

function Mostrar-Paso {
    param([int] $Numero, [int] $Total, [string] $Titulo)
    Write-Host ""
    Write-Host ("  [{0}/{1}] {2}" -f $Numero, $Total, $Titulo) -ForegroundColor Cyan
    Write-Host ("  " + ("-" * 66)) -ForegroundColor DarkGray
    Escribir-Log "===== PASO $Numero/$Total : $Titulo ====="
}

function Ok       { param([string] $T) Write-Host "      [OK]    $T" -ForegroundColor Green;  Escribir-Log $T "OK" }
function Info     { param([string] $T) Write-Host "      ->      $T" -ForegroundColor Gray;   Escribir-Log $T }
function Saltado  { param([string] $T) Write-Host "      [YA]    $T" -ForegroundColor DarkGray; Escribir-Log "$T (ya estaba hecho)" }
function Aviso    {
    param([string] $T)
    Write-Host "      [AVISO] $T" -ForegroundColor Yellow
    Escribir-Log $T "AVISO"
    $script:Advertencias += $T
}
function Pendiente {
    param([string] $T)
    Write-Host "      [FALTA] $T" -ForegroundColor Magenta
    Escribir-Log $T "PENDIENTE"
    $script:Pendientes += $T
}
function Fallo {
    param([string] $T, [string] $Sugerencia)
    Write-Host ""
    Write-Host "      [ERROR] $T" -ForegroundColor Red
    if ($Sugerencia) { Write-Host "              $Sugerencia" -ForegroundColor Yellow }
    Escribir-Log "$T | $Sugerencia" "ERROR"
}

# ---------------------------------------------------------------------------
#  Estado: permite reanudar sin repetir trabajo
# ---------------------------------------------------------------------------

function Leer-Estado {
    if (-not (Test-Path $archivoEstado)) { return @{} }
    try {
        $tabla = @{}
        $json = Get-Content $archivoEstado -Raw -Encoding UTF8 | ConvertFrom-Json
        foreach ($p in $json.PSObject.Properties) { $tabla[$p.Name] = $p.Value }
        return $tabla
    } catch {
        # Un estado corrupto no debe impedir instalar: se empieza de cero.
        return @{}
    }
}

function Esta-Hecho {
    param([string] $Clave)
    return $script:Estado.ContainsKey($Clave) -and $script:Estado[$Clave]
}

function Marcar-Hecho {
    param([string] $Clave)
    if ($SoloVerificar) { return }
    $script:Estado[$Clave] = $true
    try {
        $script:Estado | ConvertTo-Json | Set-Content -Path $archivoEstado -Encoding UTF8
    } catch {
        Escribir-Log "No se pudo guardar el estado: $_" "AVISO"
    }
}

# ---------------------------------------------------------------------------
#  Utilidades resistentes a fallos
# ---------------------------------------------------------------------------

# Reintenta una accion con esperas crecientes. Devuelve $true si logro pasar.
function Reintentar {
    param(
        [scriptblock] $Accion,
        [string] $Descripcion,
        [int] $Intentos = 3,
        [int] $EsperaBase = 5
    )
    for ($i = 1; $i -le $Intentos; $i++) {
        try {
            & $Accion
            return $true
        } catch {
            $msg = $_.Exception.Message
            Escribir-Log "Intento $i/$Intentos de '$Descripcion' fallo: $msg" "AVISO"
            if ($i -lt $Intentos) {
                $espera = $EsperaBase * $i
                Write-Host "      ... fallo el intento $i de $Intentos. Reintentando en $espera s." -ForegroundColor Yellow
                Write-Host "          ($msg)" -ForegroundColor DarkGray
                Start-Sleep -Seconds $espera
            } else {
                Write-Host "      ... agotados los $Intentos intentos." -ForegroundColor Red
                Write-Host "          ($msg)" -ForegroundColor DarkGray
            }
        }
    }
    return $false
}

# NOTA sobre las descargas grandes: no las hace este script a mano, sino las
# herramientas que ya saben reanudarlas y verificarlas:
#   - Los programas base (Java, Python) -> winget, que valida el paquete.
#   - Las librerias de Python (~2 GB)   -> pip con una cache propia en
#     logs\pip-cache: lo ya descargado no se vuelve a bajar, y las fases
#     completadas quedan anotadas en instalacion-estado.json.
#   - Los modelos de reconocimiento      -> los baja DeepFace; aqui se limpian
#     los restos .part de intentos anteriores y se reutilizan si ya estaban.

# Ejecuta un programa externo y devuelve su salida como TEXTO LIMPIO junto con el
# codigo de salida. Necesario porque PowerShell 5.1, al redirigir 2>&1, convierte
# las lineas de error en objetos ErrorRecord que ensucian la salida.
function Ejecutar-Nativo {
    param(
        [string]   $Programa,
        [string[]] $Argumentos = @()
    )
    $lineas = & $Programa @Argumentos 2>&1 | ForEach-Object {
        if ($_ -is [System.Management.Automation.ErrorRecord]) { $_.Exception.Message } else { $_.ToString() }
    }
    return [pscustomobject]@{
        Texto  = ($lineas -join "`n")
        Codigo = $LASTEXITCODE
    }
}

function Probar-Puerto {
    param([int] $Puerto)
    return $null -ne (Get-NetTCPConnection -LocalPort $Puerto -State Listen -ErrorAction SilentlyContinue)
}

function Hay-Internet {
    try {
        return (Test-NetConnection-Simple "pypi.org" 443)
    } catch { return $false }
}

function Test-NetConnection-Simple {
    param([string] $Host_, [int] $Puerto)
    try {
        $cliente = New-Object System.Net.Sockets.TcpClient
        $tarea = $cliente.ConnectAsync($Host_, $Puerto)
        $listo = $tarea.Wait(6000)
        $cliente.Close()
        return $listo
    } catch { return $false }
}

function Buscar-Programa {
    param([string] $Comando, [string[]] $RutasExtra = @())
    $encontrado = Get-Command $Comando -ErrorAction SilentlyContinue
    if ($encontrado) { return $encontrado.Source }
    foreach ($r in $RutasExtra) {
        $expandida = Get-ChildItem -Path $r -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($expandida) { return $expandida.FullName }
    }
    return $null
}

function Instalar-Con-Winget {
    param([string] $Id, [string] $Nombre)
    $winget = Get-Command winget -ErrorAction SilentlyContinue
    if (-not $winget) {
        Aviso "No hay winget en esta PC: hay que instalar $Nombre a mano."
        return $false
    }
    Info "Instalando $Nombre con winget (puede tardar varios minutos)..."
    $accion = {
        $p = Start-Process -FilePath "winget" -ArgumentList @(
            "install", "--id", $Id, "--exact", "--silent",
            "--accept-package-agreements", "--accept-source-agreements"
        ) -Wait -PassThru -NoNewWindow
        # 0 = instalado, -1978335189 = ya estaba instalado
        if ($p.ExitCode -ne 0 -and $p.ExitCode -ne -1978335189) {
            throw "winget devolvio el codigo $($p.ExitCode)"
        }
    }
    return (Reintentar -Accion $accion -Descripcion "instalacion de $Nombre" -Intentos 2 -EsperaBase 10)
}

# Descarga un archivo con reintentos. Guarda en .part y solo lo da por bueno si
# el tamano coincide con el que anuncio el servidor.
function Descargar-Archivo {
    param([string] $Url, [string] $Destino, [string] $Nombre = "archivo")

    if (Test-Path $Destino) { return $true }
    $parcial = "$Destino.part"

    $bajar = {
        if (Test-Path $parcial) { Remove-Item $parcial -Force -ErrorAction SilentlyContinue }
        $antes = $ProgressPreference
        $ProgressPreference = "Continue"
        try {
            Invoke-WebRequest -Uri $Url -OutFile $parcial -UseBasicParsing -TimeoutSec 180 -ErrorAction Stop
        } finally { $ProgressPreference = $antes }

        # Comprobacion de que llego completo.
        $esperado = 0
        try {
            $cab = Invoke-WebRequest -Uri $Url -Method Head -UseBasicParsing -TimeoutSec 30 -ErrorAction Stop
            $esperado = [int64]$cab.Headers['Content-Length']
        } catch { }
        $real = (Get-Item $parcial).Length
        if ($esperado -gt 0 -and $real -ne $esperado) {
            Remove-Item $parcial -Force -ErrorAction SilentlyContinue
            throw "la descarga quedo incompleta ($real de $esperado bytes)"
        }
        Move-Item -Path $parcial -Destination $Destino -Force -ErrorAction Stop
    }

    return (Reintentar -Accion $bajar -Descripcion "descarga de $Nombre" -Intentos 3 -EsperaBase 8)
}

# Instala Python 3.12 SIN depender de winget, que en equipos administrados
# (laboratorios, dominios) suele estar restringido por politica.
# Orden: 1) el instalador que viene incluido  2) descarga de python.org
#        3) winget, como ultimo recurso.
function Instalar-Python {
    $carpetaReq = Join-Path $Carpeta "requisitos"
    $instaladorPy = $null

    # 1) ¿Vino incluido en el paquete?
    if (Test-Path $carpetaReq) {
        $instaladorPy = Get-ChildItem (Join-Path $carpetaReq "python-3.12*-amd64.exe") -ErrorAction SilentlyContinue |
                        Select-Object -First 1 | ForEach-Object { $_.FullName }
        if ($instaladorPy) { Ok "Se usara el instalador de Python incluido en el paquete." }
    }

    # 2) Descargarlo de python.org
    if (-not $instaladorPy) {
        $descargas = Join-Path $carpetaLogs "descargas"
        if (-not (Test-Path $descargas)) { New-Item -ItemType Directory -Path $descargas -Force | Out-Null }
        $destino = Join-Path $descargas "python-$VERSION_PYTHON-amd64.exe"
        Info "Descargando Python $VERSION_PYTHON de python.org (25 MB)..."
        if (Descargar-Archivo "https://www.python.org/ftp/python/$VERSION_PYTHON/python-$VERSION_PYTHON-amd64.exe" `
                              $destino "Python $VERSION_PYTHON") {
            $instaladorPy = $destino
        }
    }

    # 3) Ejecutarlo.
    #    Se usa /passive y NO /quiet: el instalador de Python muestra su propia
    #    barra de progreso, sin pedir nada. Con /quiet no se ve absolutamente
    #    nada y no hay forma de distinguir "trabajando" de "colgado".
    if ($instaladorPy) {
        $registroPy = Join-Path $carpetaLogs "python-instalacion.log"
        Info "Instalando Python. Aparecera su propia ventana de progreso."
        Info "Suele tardar 2-5 minutos. No la cierres."

        $ejecutar = {
            $p = Start-Process -FilePath $instaladorPy -PassThru -ArgumentList @(
                "/passive", "/log", "`"$registroPy`"",
                "InstallAllUsers=1", "PrependPath=1",
                "Include_launcher=1", "InstallLauncherAllUsers=1", "Include_test=0"
            )

            # Espera con senales de vida y un limite: si se cuelga, no deja
            # el instalador esperando para siempre.
            $limiteSegundos = 900          # 15 minutos
            $transcurrido = 0
            while (-not $p.HasExited -and $transcurrido -lt $limiteSegundos) {
                Start-Sleep -Seconds 15
                $transcurrido += 15
                if ($transcurrido % 60 -eq 0) {
                    Write-Host ("        ... sigue instalando ({0} min)" -f ($transcurrido / 60)) -ForegroundColor DarkGray
                }
            }

            if (-not $p.HasExited) {
                try { $p.Kill() } catch { }
                throw "el instalador de Python no termino en 15 minutos; se cancelo"
            }
            # 0 = instalado, 3010 = instalado pero pide reinicio
            if ($p.ExitCode -ne 0 -and $p.ExitCode -ne 3010) {
                throw "el instalador de Python devolvio el codigo $($p.ExitCode) (detalle en logs\python-instalacion.log)"
            }
        }
        if (Reintentar -Accion $ejecutar -Descripcion "instalacion de Python" -Intentos 2 -EsperaBase 10) {
            Refrescar-Path
            return $true
        }
        Aviso "Puedes instalar Python a mano: $instaladorPy"
        Aviso "Marca 'Add Python to PATH' y luego vuelve a ejecutar este instalador."
    }

    # 4) Ultimo recurso: winget
    Aviso "Se intentara con winget como ultimo recurso."
    if (Instalar-Con-Winget "Python.Python.3.12" "Python 3.12") {
        Refrescar-Path
        return $true
    }
    return $false
}

function Refrescar-Path {
    # Tras instalar algo, el PATH de esta sesion no lo conoce todavia.
    $maquina = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
    $usuario = [System.Environment]::GetEnvironmentVariable("Path", "User")
    $env:Path = "$maquina;$usuario"
}

# ---------------------------------------------------------------------------
#  Arranque
# ---------------------------------------------------------------------------

if (-not (Test-Path $carpetaLogs)) { New-Item -ItemType Directory -Path $carpetaLogs -Force | Out-Null }
$script:Estado = Leer-Estado

Clear-Host
Write-Host ""
Write-Host "  ==================================================================" -ForegroundColor Cyan
Write-Host "     CASA KETTELER - Instalacion del sistema" -ForegroundColor Cyan
Write-Host "  ==================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "     Carpeta:  $Carpeta" -ForegroundColor Gray
Write-Host "     Registro: $archivoLog" -ForegroundColor Gray
if ($SoloVerificar) {
    Write-Host ""
    Write-Host "     MODO REVISION: no se modificara nada." -ForegroundColor Yellow
}
if ($script:Estado.Count -gt 0 -and -not $SoloVerificar) {
    Write-Host ""
    Write-Host "     Se encontro una instalacion previa a medias:" -ForegroundColor Yellow
    Write-Host "     se continuara donde quedo (no se repite lo ya hecho)." -ForegroundColor Yellow
}
Write-Host ""

Escribir-Log "########## Inicio de instalacion en $Carpeta ##########"

$TOTAL = 9

# ===========================================================================
#  PASO 1 - Comprobaciones del sistema
# ===========================================================================
Mostrar-Paso 1 $TOTAL "Comprobaciones del sistema"

$esAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()
           ).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if ($esAdmin) {
    Ok "Se esta ejecutando como administrador."
} else {
    if ($SoloVerificar) {
        Aviso "Sin permisos de administrador (en modo revision no importa)."
    } else {
        Fallo "Hacen falta permisos de administrador." `
              "Cierra esta ventana y usa clic derecho -> 'Ejecutar como administrador' sobre instalar.bat"
        exit 1
    }
}

$unidad = (Get-Item $Carpeta).PSDrive
if ($unidad) {
    $libreGB = [Math]::Round($unidad.Free / 1GB, 1)
    if ($libreGB -lt $ESPACIO_MINIMO_GB) {
        Fallo "Solo hay $libreGB GB libres en $($unidad.Name): y se necesitan $ESPACIO_MINIMO_GB GB." `
              "Libera espacio (el entorno de Python ocupa unos 2 GB y los modelos otro tanto) y vuelve a ejecutar."
        if (-not $SoloVerificar) { exit 1 }
    } else {
        Ok "Espacio en disco suficiente ($libreGB GB libres)."
    }
}

if (Hay-Internet) {
    Ok "Hay conexion a Internet."
} else {
    Aviso "No se detecta Internet. Las descargas fallaran; si ya estaba todo bajado, puede continuar."
}

Info "Windows: $((Get-CimInstance Win32_OperatingSystem).Caption)"

# ===========================================================================
#  PASO 2 - Programas base
# ===========================================================================
Mostrar-Paso 2 $TOTAL "Programas base (Java, Python, MySQL)"

# --- Java 21 ---
$javaOk = $false
$java = Buscar-Programa "java"
if ($java) {
    $salida = (Ejecutar-Nativo "java" @("-version")).Texto
    if ($salida -match '(?:version "|openjdk )(\d+)') {
        $verJava = [int]$Matches[1]
        if ($verJava -ge $JAVA_MINIMO) {
            Ok "Java $verJava detectado."
            $javaOk = $true
        } else {
            Aviso "Java $verJava es demasiado antiguo (se necesita $JAVA_MINIMO o superior)."
        }
    } else {
        Aviso "Hay un java instalado pero no se pudo leer su version."
    }
}
if (-not $javaOk -and -not $SoloVerificar) {
    if (Instalar-Con-Winget "Microsoft.OpenJDK.21" "Java (OpenJDK 21)") {
        Refrescar-Path
        if (Buscar-Programa "java") { Ok "Java instalado."; $javaOk = $true }
    }
}
if (-not $javaOk) { Pendiente "Instalar JDK 21 (https://learn.microsoft.com/java/openjdk/download)" }

# --- Python 3.10 a 3.12 ---
# En una PC puede haber VARIAS versiones de Python a la vez, y el 'python' del
# PATH no suele ser la que queremos (las dependencias estan fijadas para 3.12).
# Por eso se pide una version concreta con el lanzador 'py -3.12', de mas nueva
# a mas antigua, y solo al final se acepta el 'python' del PATH.
$pythonOk   = $false
$pythonExe  = $null
$pythonArgs = @()

$candidatos = @()
foreach ($v in ($PYTHON_SOPORTADO | Sort-Object -Descending)) {
    $candidatos += @{ Programa = "py"; Previos = @("-$v"); Etiqueta = "py -$v" }
}
$candidatos += @{ Programa = "python"; Previos = @(); Etiqueta = "python (del PATH)" }

foreach ($c in $candidatos) {
    $p = Buscar-Programa $c.Programa
    if (-not $p) { continue }
    $argumentos = $c.Previos + @("-c", "import sys; print('%d.%d' % sys.version_info[:2])")
    $r = Ejecutar-Nativo $p $argumentos
    if ($r.Codigo -ne 0) { continue }
    $v = $r.Texto.Trim()
    if ($PYTHON_SOPORTADO -contains $v) {
        Ok "Python $v detectado ($($c.Etiqueta))."
        $pythonExe = $p; $pythonArgs = $c.Previos; $pythonOk = $true
        break
    }
}

if (-not $pythonOk -and -not $SoloVerificar) {
    if (Instalar-Python) {
        # Se vuelve a buscar, igual que arriba: puede haber quedado como
        # "py -3.12" o como el "python" del PATH.
        foreach ($c in $candidatos) {
            $p = Buscar-Programa $c.Programa
            if (-not $p) { continue }
            $r = Ejecutar-Nativo $p ($c.Previos + @("-c", "import sys; print('%d.%d' % sys.version_info[:2])"))
            if ($r.Codigo -eq 0 -and ($PYTHON_SOPORTADO -contains $r.Texto.Trim())) {
                $pythonExe = $p; $pythonArgs = $c.Previos; $pythonOk = $true
                Ok "Python $($r.Texto.Trim()) instalado ($($c.Etiqueta))."
                break
            }
        }
        if (-not $pythonOk) {
            Aviso "Python quedo instalado pero aun no aparece en el PATH de esta ventana."
            Pendiente "Cierra esta ventana y vuelve a ejecutar el instalador: Python ya estara disponible."
        }
    }
}
if (-not $pythonOk) {
    Pendiente "Instalar Python 3.12 marcando 'Add Python to PATH' (https://www.python.org/downloads/)"
}

# --- MySQL 8 ---
# No se instala en silencio a proposito: el instalador de MySQL pide configurar
# la contraseña de root de forma interactiva y automatizarlo es poco fiable.
$mysqlExe = Buscar-Programa "mysql" @(
    "C:\Program Files\MySQL\MySQL Server 8*\bin\mysql.exe",
    "C:\Program Files (x86)\MySQL\MySQL Server 8*\bin\mysql.exe"
)
$mysqlOk = $false
if ($mysqlExe) {
    Ok "MySQL detectado en $mysqlExe"
    $mysqlOk = $true
} else {
    Pendiente "Instalar MySQL Server 8 y anotar la contraseña de root (https://dev.mysql.com/downloads/installer/)"
    Aviso "MySQL se instala a mano porque su asistente pide definir la contraseña de root."
}

$mysqldumpExe = $null
if ($mysqlOk) {
    $mysqldumpExe = Join-Path (Split-Path $mysqlExe) "mysqldump.exe"
    if (Test-Path $mysqldumpExe) { Ok "mysqldump disponible (respaldos automaticos)." }
    else { Aviso "No se encontro mysqldump.exe junto a mysql.exe: los respaldos de la base de datos no funcionaran."; $mysqldumpExe = $null }
}

if ($script:Pendientes.Count -gt 0 -and -not $SoloVerificar) {
    Write-Host ""
    Write-Host "      Faltan programas base. Instalalos y vuelve a ejecutar este" -ForegroundColor Yellow
    Write-Host "      instalador: continuara donde quedo." -ForegroundColor Yellow
    Write-Host ""
    Escribir-Log "Instalacion detenida: faltan programas base." "ERROR"
    exit 1
}

# ===========================================================================
#  PASO 3 - Carpetas del sistema
# ===========================================================================
Mostrar-Paso 3 $TOTAL "Carpetas del sistema"

$carpetas = @("storage", "temp", "backups", "logs", "frontend", "target")
foreach ($c in $carpetas) {
    $ruta = Join-Path $Carpeta $c
    if (Test-Path $ruta) {
        Saltado "$c\"
    } elseif ($SoloVerificar) {
        Pendiente "Falta la carpeta $c\"
    } else {
        New-Item -ItemType Directory -Path $ruta -Force | Out-Null
        Ok "Creada $c\"
    }
}

# Avisos sobre los archivos que deben traerse compilados.
$jar = Get-ChildItem (Join-Path $Carpeta "target\*.jar") -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jar) { Ok "Backend encontrado: $($jar.Name)" }
else { Pendiente "Copiar el JAR del backend en target\ (generado con .\mvnw.cmd clean package)" }

if (Test-Path (Join-Path $Carpeta "frontend\index.html")) {
    Ok "Interfaz web encontrada en frontend\"
} else {
    Pendiente "Copiar la interfaz web compilada en frontend\ (contenido de dist\front-ketteler\browser\)"
}

if (Test-Path (Join-Path $Carpeta "python_scripts\ServidorReconocimiento.py")) {
    Ok "Scripts de reconocimiento facial encontrados."
} else {
    Pendiente "Copiar la carpeta python_scripts\ (sin venv_perfecto)"
}

# ===========================================================================
#  PASO 4 - Archivo de configuracion (.env)
# ===========================================================================
Mostrar-Paso 4 $TOTAL "Archivo de configuracion (.env)"

$archivoEnv = Join-Path $Carpeta ".env"
$claves = [ordered]@{}

if (Test-Path $archivoEnv) {
    # Se respeta lo que ya existe: solo se completa lo que falte.
    Get-Content $archivoEnv | ForEach-Object {
        $l = $_.Trim()
        if ($l -and -not $l.StartsWith("#") -and $l.Contains("=")) {
            $i = $l.IndexOf("=")
            $claves[$l.Substring(0, $i).Trim()] = $l.Substring($i + 1).Trim()
        }
    }
    Ok ".env existente leido ($($claves.Count) valores). No se sobrescribira lo que ya tiene."
} else {
    Info "No hay .env: se creara uno nuevo."
}

# IP de esta PC en la red local (para CORS y para que el celular la alcance).
# Se descartan las direcciones 169.254.x.x (las que Windows se inventa cuando no
# hay DHCP) y la de loopback: no sirven para que el celular alcance al servidor.
$ipLocal = $null
try {
    $validas = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
               Where-Object {
                   $_.IPAddress -notlike "169.254.*" -and
                   $_.IPAddress -notlike "127.*" -and
                   $_.PrefixOrigin -ne "WellKnown"
               }
    # Se prefiere la interfaz que tiene la salida a la red (ruta por defecto).
    $rutaDefecto = Get-NetRoute -DestinationPrefix "0.0.0.0/0" -ErrorAction SilentlyContinue |
                   Sort-Object RouteMetric
    foreach ($r in $rutaDefecto) {
        $coincide = $validas | Where-Object { $_.InterfaceIndex -eq $r.ifIndex } | Select-Object -First 1
        if ($coincide) { $ipLocal = $coincide.IPAddress; break }
    }
    # Si no hubo coincidencia, se toma la primera direccion util (DHCP antes que fija).
    if (-not $ipLocal -and $validas) {
        $ipLocal = ($validas | Sort-Object { if ($_.PrefixOrigin -eq "Dhcp") { 0 } else { 1 } } |
                    Select-Object -First 1).IPAddress
    }
} catch { }

if ($ipLocal) {
    Ok "IP de esta PC en la red local: $ipLocal"
    Aviso "Reserva esta IP en el router (IP fija). Si cambia, hay que regenerar el APK de los residentes."
} else {
    Aviso "No se pudo detectar una IP de red util; revisa CORS_ALLOWED_ORIGINS en el .env."
}

function Poner-Clave {
    param([string] $Nombre, [string] $Valor, [switch] $NoMostrar)
    if ($claves.Contains($Nombre) -and $claves[$Nombre] -and
        $claves[$Nombre] -notmatch "^(REEMPLAZAR|tu_correo_real|C:/ruta)") {
        Saltado "$Nombre ya estaba definido."
        return
    }
    $claves[$Nombre] = $Valor
    if ($NoMostrar) { Ok "$Nombre generado." } else { Ok "$Nombre = $Valor" }
}

if (-not $SoloVerificar) {
    # Contraseña de MySQL: se pide y se comprueba de verdad contra el servidor.
    $passMysql = $null
    if ($claves.Contains("DB_PASSWORD") -and $claves["DB_PASSWORD"] -and $claves["DB_PASSWORD"] -ne "root") {
        $passMysql = $claves["DB_PASSWORD"]
        Saltado "Contraseña de MySQL tomada del .env existente."
    } elseif ($Desatendido) {
        Aviso "Modo desatendido: no se puede pedir la contraseña de MySQL."
    } else {
        for ($i = 1; $i -le 3; $i++) {
            Write-Host ""
            Write-Host "      Escribe la contraseña de 'root' de MySQL de esta PC:" -ForegroundColor White
            $segura = Read-Host "      Contraseña" -AsSecureString
            $passMysql = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
                          [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura))
            $prueba = Ejecutar-Nativo $mysqlExe @("-uroot", "-p$passMysql", "-e", "SELECT 1;")
            if ($prueba.Codigo -eq 0) { Ok "Contraseña verificada contra MySQL."; break }
            Write-Host "      Esa contraseña no funciona." -ForegroundColor Red
            if ($i -eq 3) { Aviso "No se verifico la contraseña de MySQL; revisa el .env luego."; }
        }
    }

    if ($passMysql) { Poner-Clave "DB_PASSWORD" $passMysql -NoMostrar }

    Poner-Clave "SPRING_PROFILES_ACTIVE" "prod"
    Poner-Clave "DB_HOST"     "localhost"
    Poner-Clave "DB_PORT"     "3306"
    Poner-Clave "DB_NAME"     $NOMBRE_BD
    Poner-Clave "DB_USERNAME" "root"

    # Clave de firma de sesiones: aleatoria y distinta en cada instalacion.
    $secreto = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 64 | ForEach-Object { [char]$_ })
    Poner-Clave "JWT_SECRET" $secreto -NoMostrar

    $rutaBarras = $Carpeta.Replace("\", "/")
    Poner-Clave "APP_STORAGE_PATH" "$rutaBarras/storage"
    Poner-Clave "APP_TEMP_PATH"    "$rutaBarras/temp"
    Poner-Clave "APP_FRONTEND_PATH" "$rutaBarras/frontend/"

    # Los modelos de reconocimiento se guardan DENTRO de la instalacion y no en
    # la carpeta del usuario. Motivo: el arranque automatico corre como SYSTEM,
    # que tiene otro perfil, y volveria a descargar los 260 MB de modelos.
    Poner-Clave "DEEPFACE_HOME" "$rutaBarras/python_scripts"

    $origenes = @("http://localhost:$PUERTO_BACKEND", "capacitor://localhost", "http://localhost")
    if ($ipLocal) { $origenes += "http://${ipLocal}:$PUERTO_BACKEND" }
    Poner-Clave "CORS_ALLOWED_ORIGINS" ($origenes -join ",")

    if (-not $claves.Contains("MAIL_USERNAME") -or -not $claves["MAIL_USERNAME"] -or
        $claves["MAIL_USERNAME"] -match "^tu_correo_real") {
        $claves["MAIL_USERNAME"] = "correo_de_la_residencia@gmail.com"
        $claves["MAIL_PASSWORD"] = "clave_de_aplicacion_de_16_digitos"
        Pendiente "Poner el correo real y su clave de aplicacion en MAIL_USERNAME / MAIL_PASSWORD del .env (se usa para enviar contraseñas temporales)"
    }

    # Se respalda el .env anterior antes de reescribirlo.
    if (Test-Path $archivoEnv) {
        $copia = Join-Path $carpetaLogs ("env-anterior-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".txt")
        Copy-Item $archivoEnv $copia -Force
        Info "Copia del .env anterior en logs\$(Split-Path $copia -Leaf)"
    }

    $texto = @("# Generado por instalar.ps1 el $(Get-Date -Format 'yyyy-MM-dd HH:mm')",
               "# No compartas este archivo: contiene contraseñas.", "")
    foreach ($k in $claves.Keys) { $texto += "$k=$($claves[$k])" }
    Set-Content -Path $archivoEnv -Value $texto -Encoding UTF8
    Ok ".env escrito con $($claves.Count) valores."
    Marcar-Hecho "env"
} else {
    if (Test-Path $archivoEnv) { Ok ".env presente." } else { Pendiente "Falta el archivo .env" }
}

# --- Ruta de mysqldump en application.properties (respaldos) ---
if ($mysqldumpExe -and -not $SoloVerificar) {
    $props = Join-Path $Carpeta "application.properties"
    $rutaDump = $mysqldumpExe.Replace("\", "/")
    if (Test-Path $props) {
        $contenido = Get-Content $props -Raw
        if ($contenido -match "app\.backup\.mysqldump=") {
            Saltado "application.properties ya define la ruta de mysqldump."
        } else {
            Add-Content -Path $props -Value "`napp.backup.mysqldump=$rutaDump"
            Ok "Ruta de mysqldump añadida a application.properties."
        }
    }
}

# ===========================================================================
#  PASO 5 - Base de datos
# ===========================================================================
Mostrar-Paso 5 $TOTAL "Base de datos"

$passBd = $claves["DB_PASSWORD"]

if (-not $mysqlOk) {
    Pendiente "No se pudo preparar la base de datos: falta MySQL."
} elseif ($SoloVerificar) {
    $existe = (Ejecutar-Nativo $mysqlExe @("-uroot", "-p$passBd", "-N", "-e", "SHOW DATABASES LIKE '$NOMBRE_BD';")).Texto
    if ($existe -match $NOMBRE_BD) { Ok "La base de datos '$NOMBRE_BD' existe." }
    else { Pendiente "Falta crear la base de datos '$NOMBRE_BD'." }
} else {
    # El servicio de MySQL debe arrancar solo con la PC.
    $servicio = Get-Service -Name "MySQL*" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($servicio) {
        if ($servicio.StartType -ne "Automatic") {
            Set-Service -Name $servicio.Name -StartupType Automatic
            Ok "Servicio $($servicio.Name) puesto en inicio automatico."
        } else { Saltado "El servicio $($servicio.Name) ya arranca solo." }
        if ($servicio.Status -ne "Running") {
            Start-Service -Name $servicio.Name
            Ok "Servicio $($servicio.Name) iniciado."
        }
    } else { Aviso "No se encontro el servicio de MySQL; revisa services.msc." }

    $crear = {
        $sql = "CREATE DATABASE IF NOT EXISTS $NOMBRE_BD CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
        $r = Ejecutar-Nativo $mysqlExe @("-uroot", "-p$passBd", "-e", $sql)
        if ($r.Codigo -ne 0) { throw $r.Texto }
    }
    if (Reintentar -Accion $crear -Descripcion "creacion de la base de datos" -Intentos 3 -EsperaBase 4) {
        Ok "Base de datos '$NOMBRE_BD' lista."
        Marcar-Hecho "bd"
    } else {
        Fallo "No se pudo crear la base de datos." "Comprueba que MySQL este encendido y que la contraseña del .env sea correcta."
    }

    # Restauracion de respaldo: solo si se pidio y la base esta vacia.
    if ($RestaurarRespaldo) {
        if (-not (Test-Path $RestaurarRespaldo)) {
            Aviso "No se encontro el respaldo '$RestaurarRespaldo'."
        } else {
            $consulta = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$NOMBRE_BD';"
            $tablas = (Ejecutar-Nativo $mysqlExe @("-uroot", "-p$passBd", "-N", "-e", $consulta)).Texto
            $cuantasTablas = 0
            [void][int]::TryParse(($tablas -split "`n")[0].Trim(), [ref]$cuantasTablas)
            if ($cuantasTablas -gt 0) {
                Aviso "La base de datos YA TIENE datos ($cuantasTablas tablas): no se restaura el respaldo, para no perder nada."
            } else {
                Info "Restaurando respaldo (puede tardar)..."
                $restaurar = {
                    $ruta = (Resolve-Path $RestaurarRespaldo).Path
                    $r = Ejecutar-Nativo "cmd" @("/c", "`"$mysqlExe`" -uroot -p$passBd $NOMBRE_BD < `"$ruta`"")
                    if ($r.Codigo -ne 0) { throw "mysql devolvio $($r.Codigo): $($r.Texto)" }
                }
                if (Reintentar -Accion $restaurar -Descripcion "restauracion del respaldo" -Intentos 2) {
                    Ok "Respaldo restaurado."
                } else { Fallo "No se pudo restaurar el respaldo." "Puedes hacerlo a mano segun DESPLIEGUE.md paso 5." }
            }
        }
    }
}

# ===========================================================================
#  PASO 6 - Entorno de Python (el paso mas largo)
# ===========================================================================
Mostrar-Paso 6 $TOTAL "Entorno de Python para el reconocimiento facial"

$carpetaPy = Join-Path $Carpeta "python_scripts"
$venv      = Join-Path $carpetaPy "venv_perfecto"
$venvPy    = Join-Path $venv "Scripts\python.exe"
$requisitos = Join-Path $carpetaPy "requirements.txt"

if (-not (Test-Path $requisitos)) {
    Pendiente "Falta python_scripts\requirements.txt: no se puede preparar el entorno."
} elseif ($SoloVerificar) {
    if (Test-Path $venvPy) {
        $prueba = (Ejecutar-Nativo $venvPy @("-c", "import torch, tensorflow, deepface, flask, cv2; print('IMPORTS_OK')")).Texto
        if ($prueba -match "IMPORTS_OK") { Ok "Entorno de Python completo y funcional." }
        else { Pendiente "El entorno de Python existe pero le faltan dependencias." }
    } else { Pendiente "Falta crear el entorno de Python (venv_perfecto)." }
} else {
    # 6.1 Crear el entorno virtual si no existe o si esta roto.
    $venvValido = (Test-Path $venvPy)
    if ($venvValido) {
        $sano = Ejecutar-Nativo $venvPy @("-c", "print(1)")
        if ($sano.Codigo -ne 0) { $venvValido = $false }
        if (-not $venvValido) {
            Aviso "El entorno de Python existente esta roto: se recreara."
            Remove-Item $venv -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
    if ($venvValido) {
        Saltado "Entorno virtual ya creado."
    } else {
        Info "Creando el entorno virtual..."
        $argsVenv = $pythonArgs + @("-m", "venv", $venv)
        $rVenv = Ejecutar-Nativo $pythonExe $argsVenv
        if ($rVenv.Codigo -ne 0) { Escribir-Log $rVenv.Texto "ERROR" }
        if (-not (Test-Path $venvPy)) {
            Fallo "No se pudo crear el entorno virtual de Python." "Comprueba que Python 3.12 este bien instalado."
            exit 1
        }
        Ok "Entorno virtual creado."
    }

    # 6.2 Instalar dependencias EN FASES.
    #     Asi se ve el progreso y, si algo falla, al reejecutar solo se repite
    #     la fase que quedo pendiente. La cache de pip evita volver a descargar.
    if (-not (Test-Path $cachePip)) { New-Item -ItemType Directory -Path $cachePip -Force | Out-Null }

    $comunes = @("--cache-dir", $cachePip, "--retries", "10", "--timeout", "120",
                 "--disable-pip-version-check")

    # Si el instalador vino con los paquetes incluidos, se instala SIN INTERNET
    # desde esa carpeta. Es lo que hace la opcion -SinInternet al construir el .exe.
    $paquetesLocales = Join-Path $Carpeta "paquetes-python"
    $sinRed = (Test-Path $paquetesLocales) -and
              ((Get-ChildItem $paquetesLocales -File -ErrorAction SilentlyContinue).Count -gt 0)

    $listaRequisitos = $requisitos
    if ($sinRed) {
        $cuantos = (Get-ChildItem $paquetesLocales -File).Count
        Ok "Se encontraron $cuantos paquetes incluidos: se instalara sin Internet."
        $origen = @("--no-index", "--find-links", $paquetesLocales)

        # requirements.txt declara el indice de PyTorch, que choca con --no-index.
        # Se usa una copia sin esa linea para que pip no intente salir a la red.
        $listaRequisitos = Join-Path $carpetaLogs "requisitos-sin-indice.txt"
        Get-Content $requisitos |
            Where-Object { $_ -notmatch '^\s*--(extra-)?index-url' } |
            Set-Content -Path $listaRequisitos -Encoding ASCII
        Info "Lista de dependencias adaptada para instalacion sin red."
    } else {
        $origen = @("--extra-index-url", $INDICE_TORCH)
    }

    $fases = @(
        @{ Clave = "pip-herramientas"; Nombre = "Herramientas de instalacion";
           Args = @("install") + $comunes + $origen + @("--upgrade", "pip", "setuptools", "wheel") },
        @{ Clave = "pip-torch";        Nombre = "PyTorch para CPU (~250 MB)";
           Args = @("install") + $comunes + $origen + @("torch==2.12.0+cpu", "torchvision==0.27.0+cpu") },
        @{ Clave = "pip-tensorflow";   Nombre = "TensorFlow (~600 MB)";
           Args = @("install") + $comunes + $origen + @("tensorflow==2.21.0", "tf_keras==2.21.0") },
        @{ Clave = "pip-resto";        Nombre = "Resto de dependencias";
           Args = @("install") + $comunes + $origen + @("-r", $listaRequisitos) }
    )

    $todoBien = $true
    foreach ($fase in $fases) {
        if (Esta-Hecho $fase.Clave) {
            Saltado "$($fase.Nombre)"
            continue
        }
        Write-Host ""
        Info "$($fase.Nombre) ..."
        $desde = Get-Date
        $argumentosPip = $fase.Args
        $instalar = {
            # Se muestra en vivo (no con Ejecutar-Nativo) para que se vea avanzar.
            & $venvPy -m pip @argumentosPip 2>&1 | ForEach-Object {
                if ($_ -is [System.Management.Automation.ErrorRecord]) { $l = $_.Exception.Message }
                else { $l = $_.ToString() }
                Escribir-Log $l "PIP"
                # Solo se muestran las lineas utiles, para no inundar la consola.
                if ($l -match "^(Collecting|Downloading|Installing|Successfully|Using cached|ERROR|WARNING)") {
                    Write-Host "        $l" -ForegroundColor DarkGray
                }
            }
            if ($LASTEXITCODE -ne 0) { throw "pip devolvio el codigo $LASTEXITCODE" }
        }
        if (Reintentar -Accion $instalar -Descripcion $fase.Nombre -Intentos 4 -EsperaBase 8) {
            $mins = [Math]::Round(((Get-Date) - $desde).TotalMinutes, 1)
            Ok "$($fase.Nombre) - listo en $mins min."
            Marcar-Hecho $fase.Clave
        } else {
            Fallo "No se pudo completar: $($fase.Nombre)" `
                  "Vuelve a ejecutar el instalador cuando tengas mejor conexion: continuara desde aqui, lo ya descargado se reutiliza."
            $todoBien = $false
            break
        }
    }

    # 6.3 Comprobar de verdad que el entorno sirve.
    if ($todoBien) {
        $prueba = (Ejecutar-Nativo $venvPy @("-c", "import torch, tensorflow, deepface, flask, cv2; print('IMPORTS_OK')")).Texto
        if ($prueba -match "IMPORTS_OK") {
            Ok "Todas las librerias cargan correctamente."
            Marcar-Hecho "python-verificado"
        } else {
            Fallo "Las librerias no cargan bien." "Detalle en logs\instalacion.log"
            Escribir-Log $prueba "ERROR"
        }
    }
}

# ===========================================================================
#  PASO 7 - Modelos de reconocimiento facial
# ===========================================================================
Mostrar-Paso 7 $TOTAL "Modelos de reconocimiento facial"

# Los modelos viven dentro de la instalacion (ver DEEPFACE_HOME en el paso 4).
$pesosDestino = Join-Path $carpetaPy ".deepface\weights"

if ($SoloVerificar -or -not (Test-Path $venvPy)) {
    if (Test-Path $pesosDestino) {
        $n = (Get-ChildItem $pesosDestino -File -Filter "*.*" -ErrorAction SilentlyContinue |
              Where-Object { $_.Extension -ne ".part" }).Count
        Ok "Hay $n archivos de modelos en la instalacion."
    } else { Pendiente "Faltan los modelos de reconocimiento facial." }
} elseif (Esta-Hecho "modelos") {
    Saltado "Modelos ya preparados."
} else {
    $env:DEEPFACE_HOME = $carpetaPy

    # Se limpian descargas a medias de intentos anteriores: un .part corrupto
    # hace fallar la carga del modelo una y otra vez.
    if (Test-Path $pesosDestino) {
        $aMedias = Get-ChildItem $pesosDestino -File -Filter "*.part" -ErrorAction SilentlyContinue
        foreach ($p in $aMedias) {
            Remove-Item $p.FullName -Force -ErrorAction SilentlyContinue
            Info "Descartada una descarga a medias: $($p.Name)"
        }
    }

    # Si esta PC ya tenia los modelos en la carpeta del usuario, se copian:
    # es cuestion de segundos frente a volver a descargar 260 MB.
    $pesosUsuario = Join-Path $env:USERPROFILE ".deepface\weights"
    if ((Test-Path $pesosUsuario) -and -not (Test-Path $pesosDestino)) {
        try {
            New-Item -ItemType Directory -Path $pesosDestino -Force | Out-Null
            Get-ChildItem $pesosUsuario -File | Where-Object { $_.Extension -ne ".part" } |
                Copy-Item -Destination $pesosDestino -Force
            $n = (Get-ChildItem $pesosDestino -File).Count
            Ok "Reutilizados $n modelos que ya estaban en esta PC (sin descargar nada)."
        } catch {
            Aviso "No se pudieron copiar los modelos existentes: se descargaran."
        }
    }

    Info "Comprobando los modelos (ArcFace, detector y anti-suplantacion)."
    Info "Si hay que descargarlos son unos 260 MB, y solo ocurre una vez."
    $codigoModelos = @(
        "import os",
        "os.environ['TF_CPP_MIN_LOG_LEVEL']='3'",
        "os.environ['TF_USE_LEGACY_KERAS']='1'",
        "from deepface import DeepFace",
        "DeepFace.build_model('ArcFace')",
        "print('MODELOS_OK')"
    ) -join "; "

    $descargarModelos = {
        $r = (Ejecutar-Nativo $venvPy @("-c", $codigoModelos)).Texto
        Escribir-Log $r "MODELOS"
        if ($r -notmatch "MODELOS_OK") { throw "no se completo la preparacion de los modelos" }
    }
    if (Reintentar -Accion $descargarModelos -Descripcion "preparacion de modelos" -Intentos 3 -EsperaBase 10) {
        Ok "Modelos listos en python_scripts\.deepface\"
        Marcar-Hecho "modelos"
    } else {
        Aviso "Los modelos no quedaron listos. Se descargaran solos en el primer reconocimiento (esa primera marca tardara mas)."
    }
}

# ===========================================================================
#  PASO 8 - Red Wi-Fi de la residencia
# ===========================================================================
Mostrar-Paso 8 $TOTAL "Red Wi-Fi de la residencia (SSID / BSSID)"

# Sin estos datos NINGUN residente puede marcar asistencia, asi que se intenta
# leerlos y se avisa con claridad si no se puede.
$ssid = $null; $bssid = $null
try {
    $wlan = (Ejecutar-Nativo "netsh" @("wlan", "show", "interfaces")).Texto
    if ($wlan -match "permiso de ubicaci|location permission") {
        Aviso "Windows no deja leer la red Wi-Fi: falta el permiso de ubicacion."
        Write-Host "              Activalo en Configuracion -> Privacidad y seguridad -> Ubicacion" -ForegroundColor Yellow
        Write-Host "              (atajo: ms-settings:privacy-location) y vuelve a ejecutar." -ForegroundColor Yellow
    } else {
        foreach ($linea in ($wlan -split "`r?`n")) {
            if ($linea -match "^\s*BSSID\s*:\s*(.+)$")      { $bssid = $Matches[1].Trim() }
            elseif ($linea -match "^\s*SSID\s*:\s*(.+)$")    { $ssid  = $Matches[1].Trim() }
        }
    }
} catch { Aviso "No se pudo consultar la red Wi-Fi: $($_.Exception.Message)" }

if ($ssid -or $bssid) {
    Ok "Red detectada - SSID: $ssid   BSSID: $bssid"
    if (-not $SoloVerificar -and $mysqlOk) {
        $filas = (Ejecutar-Nativo $mysqlExe @("-uroot", "-p$passBd", "-N", "-e", "SELECT COUNT(*) FROM $NOMBRE_BD.tresidence;")).Texto
        $cuantas = 0
        [void][int]::TryParse((($filas -split "`n")[0]).Trim(), [ref]$cuantas)
        if ($cuantas -gt 0) {
            $sql = "UPDATE $NOMBRE_BD.tresidence SET wifiSsid='$ssid', wifiBssid='$bssid';"
            $r = Ejecutar-Nativo $mysqlExe @("-uroot", "-p$passBd", "-e", $sql)
            if ($r.Codigo -eq 0) { Ok "Red guardada en la base de datos." }
            else { Pendiente "Guardar el SSID/BSSID a mano (DESPLIEGUE.md paso 5.1)." }
        } else {
            Info "Todavia no hay ninguna residencia registrada en la base de datos."
            Pendiente "Tras registrar la residencia en el sistema, guardar SSID='$ssid' y BSSID='$bssid' (DESPLIEGUE.md paso 5.1)"
            $datos = "SSID=$ssid`r`nBSSID=$bssid`r`n"
            Set-Content -Path (Join-Path $carpetaLogs "red-wifi-detectada.txt") -Value $datos -Encoding UTF8
            Info "Datos guardados en logs\red-wifi-detectada.txt para usarlos luego."
        }
    }
} else {
    Pendiente "Registrar el SSID/BSSID de la residencia (DESPLIEGUE.md paso 5.1). Sin esto nadie podra marcar asistencia."
}

# ===========================================================================
#  PASO 9 - Arranque automatico y verificacion final
# ===========================================================================
Mostrar-Paso 9 $TOTAL "Arranque automatico y verificacion"

$lanzador = Join-Path $Carpeta "iniciar-casa-ketteler.bat"

if ($SoloVerificar) {
    $tarea = Get-ScheduledTask -TaskName $TAREA_PROGRAMADA -ErrorAction SilentlyContinue
    if ($tarea) { Ok "La tarea de arranque automatico existe." }
    else { Pendiente "Falta la tarea de arranque automatico." }
} elseif (-not (Test-Path $lanzador)) {
    Pendiente "Falta iniciar-casa-ketteler.bat: no se puede registrar el arranque automatico."
} else {
    $registrar = {
        $accion   = New-ScheduledTaskAction -Execute $lanzador -WorkingDirectory $Carpeta
        $disparo  = New-ScheduledTaskTrigger -AtStartup
        $ajustes  = New-ScheduledTaskSettingsSet -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1) `
                        -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
        $permisos = New-ScheduledTaskPrincipal -UserId "SYSTEM" -RunLevel Highest
        Register-ScheduledTask -TaskName $TAREA_PROGRAMADA -Action $accion -Trigger $disparo `
            -Settings $ajustes -Principal $permisos -Force -ErrorAction Stop | Out-Null
    }
    if (Reintentar -Accion $registrar -Descripcion "registro de la tarea programada" -Intentos 2) {
        Ok "Arranque automatico registrado (tarea '$TAREA_PROGRAMADA')."
        Marcar-Hecho "tarea"
    } else {
        Pendiente "Registrar el arranque automatico a mano (DESPLIEGUE.md paso 8)."
    }

    # --- Verificacion real: arrancar y comprobar que responde ---
    if ($jar -and (Test-Path $venvPy) -and $script:Pendientes.Count -eq 0) {
        Write-Host ""
        Info "Arrancando el sistema para comprobarlo..."
        Start-Process -FilePath $lanzador -WorkingDirectory $Carpeta -WindowStyle Minimized
        Write-Host "      Esperando a que responda (hasta 90 s; el reconocimiento tarda en cargar)..." -ForegroundColor Gray

        $backendOk = $false; $pythonListo = $false
        for ($s = 0; $s -lt 90; $s += 3) {
            Start-Sleep -Seconds 3
            if (-not $backendOk -and (Probar-Puerto $PUERTO_BACKEND)) { $backendOk = $true; Ok "Backend escuchando en el puerto $PUERTO_BACKEND." }
            if (-not $pythonListo -and (Probar-Puerto $PUERTO_PYTHON)) { $pythonListo = $true; Ok "Reconocimiento facial escuchando en el puerto $PUERTO_PYTHON." }
            if ($backendOk -and $pythonListo) { break }
        }

        if ($backendOk) {
            try {
                $r = Invoke-WebRequest -Uri "http://localhost:$PUERTO_BACKEND/casaketteler/attendance/health" `
                        -UseBasicParsing -TimeoutSec 15
                if ($r.StatusCode -eq 200) { Ok "El sistema responde correctamente (health = 200)." }
            } catch {
                Aviso "El puerto esta abierto pero la comprobacion de salud fallo: $($_.Exception.Message)"
            }
        } else {
            Fallo "El backend no arranco." "Revisa logs\backend.error.log"
        }
        if (-not $pythonListo) {
            Aviso "El reconocimiento facial no arranco todavia. Revisa logs\reconocimiento.error.log"
        }
    } else {
        Info "Se omite la prueba de arranque porque quedan cosas pendientes."
    }
}

# ===========================================================================
#  Resumen
# ===========================================================================
$duracion = [Math]::Round(((Get-Date) - $inicio).TotalMinutes, 1)

Write-Host ""
Write-Host "  ==================================================================" -ForegroundColor Cyan
Write-Host "     RESUMEN" -ForegroundColor Cyan
Write-Host "  ==================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "     Duracion: $duracion minutos" -ForegroundColor Gray
Write-Host "     Registro: $archivoLog" -ForegroundColor Gray

if ($script:Advertencias.Count -gt 0) {
    Write-Host ""
    Write-Host "     Avisos:" -ForegroundColor Yellow
    foreach ($a in $script:Advertencias) { Write-Host "       - $a" -ForegroundColor Yellow }
}

if ($script:Pendientes.Count -gt 0) {
    Write-Host ""
    Write-Host "     FALTA HACER:" -ForegroundColor Magenta
    foreach ($p in $script:Pendientes) { Write-Host "       - $p" -ForegroundColor Magenta }
    Write-Host ""
    Write-Host "     Resuelve lo de arriba y vuelve a ejecutar el instalador:" -ForegroundColor White
    Write-Host "     continuara donde quedo, sin repetir las descargas." -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "     Instalacion completa." -ForegroundColor Green
    Write-Host ""
    Write-Host "     Entrar al sistema:  http://localhost:$PUERTO_BACKEND" -ForegroundColor White
    if ($ipLocal) {
        Write-Host "     Desde otra PC:      http://${ipLocal}:$PUERTO_BACKEND" -ForegroundColor White
    }
    Write-Host "     Encender / apagar:  iniciar-casa-ketteler.bat / detener-casa-ketteler.bat" -ForegroundColor White
    Write-Host ""
    Write-Host "     Reinicia la PC para comprobar que el sistema vuelve solo." -ForegroundColor Yellow
}

Write-Host ""
Escribir-Log "########## Fin ($duracion min). Pendientes: $($script:Pendientes.Count). Avisos: $($script:Advertencias.Count) ##########"

if (-not $Desatendido) {
    Write-Host "     Presiona ENTER para cerrar." -ForegroundColor DarkGray
    Read-Host | Out-Null
}

# Codigo de salida:  0 = todo listo   2 = quedan cosas pendientes
if ($script:Pendientes.Count -gt 0) { exit 2 } else { exit 0 }
