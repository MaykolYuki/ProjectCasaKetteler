@echo off
REM ============================================================
REM  CASA KETTELER - Iniciar el sistema
REM  Doble clic en este archivo para encender todo.
REM ============================================================
title Casa Ketteler - Estado del sistema
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0iniciar-casa-ketteler.ps1"
