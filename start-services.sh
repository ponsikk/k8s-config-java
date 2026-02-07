#!/bin/bash
set -e

echo "================================================================================"
echo "  TRANSACTION SYSTEM - START ALL SPRING BOOT SERVICES"
echo "================================================================================"
echo ""

# Check if JARs exist
if [ ! -f "services/gateway-service/target/gateway-service-1.0.0.jar" ]; then
    echo "ERROR: JARs not found. Please run ./build.sh first."
    exit 1
fi

echo "Starting all services in background..."
echo ""

echo "[1/4] Starting Gateway Service (port 8080)..."
nohup java -jar services/gateway-service/target/gateway-service-1.0.0.jar > logs/gateway.log 2>&1 &
echo $! > logs/gateway.pid

echo "[2/4] Starting Fraud Detection Service (port 8081)..."
nohup java -jar services/fraud-detection-service/target/fraud-detection-service-1.0.0.jar > logs/fraud.log 2>&1 &
echo $! > logs/fraud.pid

echo "[3/4] Starting Analytics Service (port 8082)..."
nohup java -jar services/analytics-service/target/analytics-service-1.0.0.jar > logs/analytics.log 2>&1 &
echo $! > logs/analytics.pid

echo "[4/4] Starting Payment Processor Service (port 8083)..."
nohup java -jar services/payment-processor-service/target/payment-processor-service-1.0.0.jar > logs/payment.log 2>&1 &
echo $! > logs/payment.pid

echo ""
echo "================================================================================"
echo "  ALL SERVICES STARTED IN BACKGROUND"
echo "================================================================================"
echo ""
echo "Services starting (wait ~30 seconds for all to be ready):"
echo "  - Gateway Service:           http://localhost:8080/actuator/health"
echo "  - Fraud Detection Service:   http://localhost:8081/actuator/health"
echo "  - Analytics Service:         http://localhost:8082/actuator/health"
echo "  - Payment Processor Service: http://localhost:8083/actuator/health"
echo ""
echo "Logs:"
echo "  - Gateway:           tail -f logs/gateway.log"
echo "  - Fraud Detection:   tail -f logs/fraud.log"
echo "  - Analytics:         tail -f logs/analytics.log"
echo "  - Payment Processor: tail -f logs/payment.log"
echo ""
echo "To stop services: ./stop-all.sh"
echo ""
