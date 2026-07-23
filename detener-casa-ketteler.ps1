# ============================================================================
#  CASA KETTELER - Detener el sistema
#  Cierra el backend principal (8001) y el reconocimiento facial (5000).
#  USO: doble clic en "detener-casa-ketteler.bat".
# ============================================================================

Write-Host ""
Write-Host "  Deteniendo Casa Ketteler..." -ForegroundColor Cyan
Write-Host ""

foreach ($puerto in 8001, 5000) {
    $ids = Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique

    if ($ids) {
        foreach ($id in $ids) {
            try {
                Stop-Process -Id $id -Force -ErrorAction Stop
                Write-Host "  Detenido el servicio del puerto $puerto" -ForegroundColor Green
            } catch {
                Write-Host "  No se pudo detener el proceso del puerto $puerto" -ForegroundColor Red
            }
        }
    } else {
        Write-Host "  El puerto $puerto ya estaba libre." -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "  Sistema detenido." -ForegroundColor Cyan
Start-Sleep -Seconds 2
