#!/bin/bash
set -e

echo "================================================================================"
echo "  TRANSACTION SYSTEM - START ALL SERVICES"
echo "================================================================================"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "ERROR: Docker is not running. Please start Docker."
    exit 1
fi

echo "[1/5] Starting infrastructure (Kafka, Redis, PostgreSQL)..."
docker-compose up -d

echo ""
echo "[2/5] Waiting for services to be ready (30 seconds)..."
sleep 30

echo ""
echo "[3/5] Checking infrastructure status..."
docker-compose ps

echo ""
echo "[4/5] Verifying Kafka topics..."
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

echo ""
echo "================================================================================"
echo "  INFRASTRUCTURE STARTED"
echo "================================================================================"
echo "  Kafka:            localhost:9092"
echo "  Kafka UI:         http://localhost:8090"
echo "  Redis:            localhost:6379"
echo "  Redis Commander:  http://localhost:8081"
echo "  PostgreSQL:       localhost:5432 (user: transactionuser, db: transactions)"
echo "================================================================================"
echo ""
echo "[5/5] To start Spring Boot services, run:"
echo "  ./start-services.sh"
echo ""
