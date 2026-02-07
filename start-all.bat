@echo off
echo ================================================================================
echo   TRANSACTION SYSTEM - START ALL SERVICES
echo ================================================================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker is not running. Please start Docker Desktop.
    exit /b 1
)

echo [1/5] Starting infrastructure (Kafka, Redis, PostgreSQL)...
docker-compose up -d

echo.
echo [2/5] Waiting for services to be ready (30 seconds)...
timeout /t 30 /nobreak

echo.
echo [3/5] Checking infrastructure status...
docker-compose ps

echo.
echo [4/5] Verifying Kafka topics...
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

echo.
echo ================================================================================
echo   INFRASTRUCTURE STARTED
echo ================================================================================
echo   Kafka:            localhost:9092
echo   Kafka UI:         http://localhost:8090
echo   Redis:            localhost:6379
echo   Redis Commander:  http://localhost:8081
echo   PostgreSQL:       localhost:5432 (user: transactionuser, db: transactions)
echo ================================================================================
echo.
echo [5/5] To start Spring Boot services, run in separate terminals:
echo.
echo   Terminal 1: cd services\gateway-service ^&^& mvn spring-boot:run
echo   Terminal 2: cd services\fraud-detection-service ^&^& mvn spring-boot:run
echo   Terminal 3: cd services\analytics-service ^&^& mvn spring-boot:run
echo   Terminal 4: cd services\payment-processor-service ^&^& mvn spring-boot:run
echo.
echo Or use: start-services.bat (to start all in background)
echo.
pause
