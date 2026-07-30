@echo off
REM ============================================================
REM  CASA KETTELER - Instalar el sistema
REM  Doble clic en este archivo para dejar la PC lista.
REM
REM  Necesita permisos de administrador (para el arranque
REM  automatico y el servicio de MySQL): si no los tiene, los
REM  pide solo.
REM
REM  Se puede volver a ejecutar sin problema: continua donde
REM  quedo y no repite las descargas ya hechas.
REM ============================================================
title Casa Ketteler - Instalacion

REM Ya somos administrador? "net session" solo funciona con permisos elevados.
net session >nul 2>&1
if %errorlevel%==0 goto instalar

echo.
echo   Se necesitan permisos de administrador. Acepta el aviso de Windows...
echo.
powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
exit /b

:instalar
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0instalar.ps1" %*
