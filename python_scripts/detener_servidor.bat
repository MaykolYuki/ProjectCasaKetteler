@echo off
REM ============================================================
REM  Detiene el servidor de reconocimiento facial matando el
REM  proceso que este escuchando en el puerto 5000.
REM  Util cuando el servidor se cuelga al cerrar y deja el
REM  puerto ocupado ("Address already in use").
REM ============================================================
setlocal enabledelayedexpansion
set PORT=5000
set FOUND=0

for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%PORT%" ^| findstr "LISTENING"') do (
    echo Matando proceso PID %%p en el puerto %PORT%...
    taskkill /PID %%p /F
    set FOUND=1
)

if "!FOUND!"=="0" (
    echo No hay ningun proceso escuchando en el puerto %PORT%.
) else (
    echo Puerto %PORT% liberado.
)

endlocal
