@echo off
echo ================================================================================
echo   TRANSACTION SYSTEM - START ALL SPRING BOOT SERVICES
echo ================================================================================
echo.

REM Check if JARs exist
if not exist "services\gateway-service\target\gateway-service-1.0.0.jar" (
    echo ERROR: JARs not found. Please run build.bat first.
    exit /b 1
)

echo Starting all services in background...
echo.

echo [1/4] Starting Gateway Service (port 8080)...
start "Gateway Service" java -jar services\gateway-service\target\gateway-service-1.0.0.jar

echo [2/4] Starting Fraud Detection Service (port 8081)...
start "Fraud Detection Service" java -jar services\fraud-detection-service\target\fraud-detection-service-1.0.0.jar

echo [3/4] Starting Analytics Service (port 8082)...
start "Analytics Service" java -jar services\analytics-service\target\analytics-service-1.0.0.jar

echo [4/4] Starting Payment Processor Service (port 8083)...
start "Payment Processor Service" java -jar services\payment-processor-service\target\payment-processor-service-1.0.0.jar

echo.
echo ================================================================================
echo   ALL SERVICES STARTED IN BACKGROUND
echo ================================================================================
echo.
echo Services starting (wait ~30 seconds for all to be ready):
echo   - Gateway Service:           http://localhost:8080/actuator/health
echo   - Fraud Detection Service:   http://localhost:8081/actuator/health
echo   - Analytics Service:         http://localhost:8082/actuator/health
echo   - Payment Processor Service: http://localhost:8083/actuator/health
echo.
echo Check if services are ready:
echo   curl http://localhost:8080/actuator/health
echo.
echo To stop services, close the terminal windows or use: taskkill /F /FI "WINDOWTITLE eq *Service*"
echo.
pause
