@echo off
echo ================================================================================
echo   TRANSACTION SYSTEM - STOP ALL
echo ================================================================================
echo.

echo [1/2] Stopping Spring Boot services...
taskkill /F /FI "WINDOWTITLE eq Gateway Service*" 2>nul
taskkill /F /FI "WINDOWTITLE eq Fraud Detection Service*" 2>nul
taskkill /F /FI "WINDOWTITLE eq Analytics Service*" 2>nul
taskkill /F /FI "WINDOWTITLE eq Payment Processor Service*" 2>nul

echo.
echo [2/2] Stopping infrastructure (Docker Compose)...
docker-compose down

echo.
echo ================================================================================
echo   ALL SERVICES STOPPED
echo ================================================================================
echo.
pause
