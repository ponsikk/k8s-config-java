#!/bin/bash

echo "================================================================================"
echo "  TRANSACTION SYSTEM - STOP ALL"
echo "================================================================================"
echo ""

echo "[1/2] Stopping Spring Boot services..."

# Stop services using PID files
if [ -f logs/gateway.pid ]; then
    kill $(cat logs/gateway.pid) 2>/dev/null && echo "  - Gateway Service stopped"
    rm logs/gateway.pid
fi

if [ -f logs/fraud.pid ]; then
    kill $(cat logs/fraud.pid) 2>/dev/null && echo "  - Fraud Detection Service stopped"
    rm logs/fraud.pid
fi

if [ -f logs/analytics.pid ]; then
    kill $(cat logs/analytics.pid) 2>/dev/null && echo "  - Analytics Service stopped"
    rm logs/analytics.pid
fi

if [ -f logs/payment.pid ]; then
    kill $(cat logs/payment.pid) 2>/dev/null && echo "  - Payment Processor Service stopped"
    rm logs/payment.pid
fi

echo ""
echo "[2/2] Stopping infrastructure (Docker Compose)..."
docker-compose down

echo ""
echo "================================================================================"
echo "  ALL SERVICES STOPPED"
echo "================================================================================"
echo ""
