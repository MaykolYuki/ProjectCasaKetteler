@echo off
REM ============================================================
REM  CASA KETTELER - Detener el sistema
REM  Doble clic en este archivo para apagar todo.
REM ============================================================
title Casa Ketteler - Detener
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0detener-casa-ketteler.ps1"
