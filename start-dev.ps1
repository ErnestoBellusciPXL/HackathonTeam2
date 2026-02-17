# Start Docker with Hot Reloading
Write-Host "-------------------------------------------" -ForegroundColor Cyan
Write-Host "   Starting Hot Refresh Environment...     " -ForegroundColor Cyan
Write-Host "-------------------------------------------" -ForegroundColor Cyan

# Use both base and dev override files
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

Write-Host "`nReady!" -ForegroundColor Green
Write-Host "Frontend (with Hot Refresh): http://localhost:5173" -ForegroundColor Cyan
Write-Host "Backend (API): http://localhost:8080" -ForegroundColor Cyan
Write-Host "Mailpit (Emails): http://localhost:8025" -ForegroundColor Yellow
Write-Host "Database (MySQL): localhost:3306" -ForegroundColor DarkCyan

Write-Host "`nTo see logs, run: docker-compose logs -f" -ForegroundColor Gray
