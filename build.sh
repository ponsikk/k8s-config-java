#!/bin/bash
set -e

echo "================================================================================"
echo "  TRANSACTION SYSTEM - BUILD ALL MODULES"
echo "================================================================================"
echo ""

echo "[1/2] Cleaning previous builds..."
mvn clean

echo ""
echo "[2/2] Building all modules (this may take a few minutes)..."
mvn install -DskipTests

echo ""
echo "================================================================================"
echo "  BUILD SUCCESSFUL"
echo "================================================================================"
echo ""
echo "Built artifacts:"
echo "  - shared-models/target/shared-models-1.0.0.jar"
echo "  - services/gateway-service/target/gateway-service-1.0.0.jar"
echo "  - services/fraud-detection-service/target/fraud-detection-service-1.0.0.jar"
echo "  - services/analytics-service/target/analytics-service-1.0.0.jar"
echo "  - services/payment-processor-service/target/payment-processor-service-1.0.0.jar"
echo "  - load-testing/target/load-tester.jar (Fat JAR)"
echo ""
echo "Next steps:"
echo "  1. Start infrastructure: ./start-all.sh"
echo "  2. Start services: ./start-services.sh"
echo "  3. Run load test: java -jar load-testing/target/load-tester.jar --rps 1000"
echo ""
