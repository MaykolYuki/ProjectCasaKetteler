# Carga las variables de entorno del .env y arranca el backend empaquetado (JAR).
# El IDE carga el .env automaticamente; al ejecutar el JAR a mano hay que hacerlo aqui.
#
# Uso (desde PowerShell):  .\ejecutar-backend.ps1
# (funciona desde cualquier carpeta: resuelve el .env y el JAR relativos a este script)
#
# NOTA para produccion: en el servidor real NO se usa este script. Alli se definen
# las variables como variables de entorno del sistema (y SPRING_PROFILES_ACTIVE=prod).

$ErrorActionPreference = "Stop"

$envFile = Join-Path $PSScriptRoot ".env"
if (-not (Test-Path $envFile)) {
    Write-Error "No se encontro .env en $PSScriptRoot"
    exit 1
}

# Cargar cada linea NOMBRE=VALOR del .env como variable de entorno del proceso.
Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $idx = $line.IndexOf("=")
        $name = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        Set-Item -Path "env:$name" -Value $value
    }
}

$jar = Join-Path $PSScriptRoot "target\projectcasaketteler-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $jar)) {
    Write-Error "No se encontro el JAR. Genera primero con: .\mvnw.cmd clean package"
    exit 1
}

Write-Host "Variables del .env cargadas. Iniciando backend en http://localhost:8001 ..."
java -jar $jar
