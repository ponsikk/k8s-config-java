#!/bin/bash
set -e

echo "================================================================================"
echo "  KUBERNETES DEPLOYMENT - TRANSACTION SYSTEM"
echo "================================================================================"
echo ""

# Configuration
NAMESPACE="transaction-system"
DOCKER_REGISTRY="${DOCKER_REGISTRY:-your-dockerhub-username}"

# Check if kubectl is configured
if ! kubectl cluster-info > /dev/null 2>&1; then
    echo "ERROR: kubectl is not configured or cluster is not reachable"
    echo "Please configure kubectl to connect to your k3s cluster"
    exit 1
fi

echo "[1/8] Creating namespace..."
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

echo ""
echo "[2/8] Building Docker images..."
echo "  - Building gateway-service..."
docker build -t ${DOCKER_REGISTRY}/gateway-service:latest -f services/gateway-service/Dockerfile services/gateway-service

echo "  - Building fraud-detection-service..."
docker build -t ${DOCKER_REGISTRY}/fraud-detection-service:latest -f services/fraud-detection-service/Dockerfile services/fraud-detection-service

echo "  - Building analytics-service..."
docker build -t ${DOCKER_REGISTRY}/analytics-service:latest -f services/analytics-service/Dockerfile services/analytics-service

echo "  - Building payment-processor-service..."
docker build -t ${DOCKER_REGISTRY}/payment-processor-service:latest -f services/payment-processor-service/Dockerfile services/payment-processor-service

echo ""
echo "[3/8] Pushing images to registry..."
docker push ${DOCKER_REGISTRY}/gateway-service:latest
docker push ${DOCKER_REGISTRY}/fraud-detection-service:latest
docker push ${DOCKER_REGISTRY}/analytics-service:latest
docker push ${DOCKER_REGISTRY}/payment-processor-service:latest

echo ""
echo "[4/8] Deploying infrastructure (Kafka, Redis, PostgreSQL)..."
kubectl apply -f k8s/statefulsets/ -n $NAMESPACE

echo ""
echo "[5/8] Waiting for StatefulSets to be ready (60 seconds)..."
sleep 60

echo ""
echo "[6/8] Deploying ConfigMaps and Secrets..."
kubectl apply -f k8s/configmaps/ -n $NAMESPACE
kubectl apply -f k8s/secrets/ -n $NAMESPACE

echo ""
echo "[7/8] Deploying application services..."
kubectl apply -f k8s/deployments/ -n $NAMESPACE
kubectl apply -f k8s/services/ -n $NAMESPACE

echo ""
echo "[8/8] Deploying HPA and PDB..."
kubectl apply -f k8s/hpa/ -n $NAMESPACE
kubectl apply -f k8s/pdb/ -n $NAMESPACE

echo ""
echo "================================================================================"
echo "  DEPLOYMENT COMPLETE"
echo "================================================================================"
echo ""
echo "Check deployment status:"
echo "  kubectl get pods -n $NAMESPACE"
echo "  kubectl get svc -n $NAMESPACE"
echo "  kubectl get hpa -n $NAMESPACE"
echo ""
echo "Access gateway service:"
echo "  kubectl port-forward -n $NAMESPACE svc/gateway-service 8080:8080"
echo ""
echo "Monitor HPA autoscaling:"
echo "  kubectl get hpa -n $NAMESPACE --watch"
echo ""
echo "View logs:"
echo "  kubectl logs -f deployment/gateway-service -n $NAMESPACE"
echo ""
