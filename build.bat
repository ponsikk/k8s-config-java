@echo off
echo ================================================================================
echo   TRANSACTION SYSTEM - BUILD ALL MODULES
echo ================================================================================
echo.

echo [1/2] Cleaning previous builds...
call mvn clean

if %errorlevel% neq 0 (
    echo ERROR: Maven clean failed
    exit /b 1
)

echo.
echo [2/2] Building all modules (this may take a few minutes)...
call mvn install -DskipTests

if %errorlevel% neq 0 (
    echo ERROR: Maven build failed
    exit /b 1
)

echo.
echo ================================================================================
echo   BUILD SUCCESSFUL
echo ================================================================================
echo.
echo Built artifacts:
echo   - shared-models\target\shared-models-1.0.0.jar
echo   - services\gateway-service\target\gateway-service-1.0.0.jar
echo   - services\fraud-detection-service\target\fraud-detection-service-1.0.0.jar
echo   - services\analytics-service\target\analytics-service-1.0.0.jar
echo   - services\payment-processor-service\target\payment-processor-service-1.0.0.jar
echo   - load-testing\target\load-tester.jar (Fat JAR)
echo.
echo Next steps:
echo   1. Start infrastructure: start-all.bat
echo   2. Start services: start-services.bat
echo   3. Run load test: java -jar load-testing\target\load-tester.jar --rps 1000
echo.
pause
