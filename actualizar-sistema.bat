@echo off
REM ============================================================
REM  CASA KETTELER - Actualizar el sistema
REM
REM  Arrastra sobre este archivo la carpeta con la version
REM  nueva, o ejecutalo y escribe la ruta cuando la pida.
REM
REM  Si la version nueva no arranca, deshace el cambio solo.
REM ============================================================
title Casa Ketteler - Actualizacion

net session >nul 2>&1
if %errorlevel%==0 goto actualizar

echo.
echo   Se necesitan permisos de administrador. Acepta el aviso de Windows...
echo.
powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath '%~f0' -ArgumentList '%1' -Verb RunAs"
exit /b

:actualizar
if "%~1"=="" goto preguntar
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0actualizar-sistema.ps1" -Desde "%~1"
exit /b

:preguntar
echo.
set /p CARPETA=  Ruta de la carpeta con la version nueva:
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0actualizar-sistema.ps1" -Desde "%CARPETA%"
