# Transaction Processing System

High-load Kubernetes pet project for practicing K8s autoscaling, resource management, and distributed systems under extreme load.

## 🎯 Project Goal

Practice with Kubernetes under high load: HPA, pod scheduling, resource management, StatefulSets, monitoring, and optimization.

**Target Load:** 30-50k msg/sec (baseline), up to 1M msg/sec (stress test)

## 📐 Architecture

### Microservices

1. **gateway-service** (Producer)
   - REST API for transaction ingestion
   - Redis rate limiting
   - Kafka producer
   - Port: 8080

2. **fraud-detection-service** (Consumer 1)
   - Fraud analysis (high amount, suspicious merchants, geo-anomalies)
   - Publishes fraud alerts
   - Port: 8081

3. **analytics-service** (Consumer 2)
   - Real-time metrics aggregation (Redis)
   - Tracks TPS, average amount, top merchants
   - Port: 8082

4. **payment-processor-service** (Consumer 3)
   - Payment processing simulation
   - PostgreSQL persistence (batch inserts)
   - Publishes payment confirmations
   - Port: 8083

### Infrastructure

- **Kafka:** 3 partitions, compression lz4
- **Redis:** Rate limiting, analytics
- **PostgreSQL:** Transaction persistence
- **Prometheus + Grafana:** Monitoring

## 🚀 Quick Start
## Need tests 
### Prerequisites

- **Java 21** (Virtual Threads support)
- **Maven 3.8+**
- **Docker** and **Docker Compose**

### 1. Start Infrastructure

```bash
# Start Kafka, Redis, PostgreSQL
docker-compose up -d

# Verify services are running
docker-compose ps

# Check Kafka topics were created
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
```

**Optional UIs:**
- Kafka UI: http://localhost:8090
- Redis Commander: http://localhost:8081

### 2. Build All Services

```bash
# Build entire Maven project
mvn clean install

# This will create JARs for:
# - gateway-service/target/gateway-service-1.0.0.jar
# - fraud-detection-service/target/fraud-detection-service-1.0.0.jar
# - analytics-service/target/analytics-service-1.0.0.jar
# - payment-processor-service/target/payment-processor-service-1.0.0.jar
# - load-testing/target/load-tester.jar
```

### 3. Run Services Locally

**Terminal 1: Gateway Service**
```bash
cd services/gateway-service
mvn spring-boot:run
```

**Terminal 2: Fraud Detection Service**
```bash
cd services/fraud-detection-service
mvn spring-boot:run
```

**Terminal 3: Analytics Service**
```bash
cd services/analytics-service
mvn spring-boot:run
```

**Terminal 4: Payment Processor Service**
```bash
cd services/payment-processor-service
mvn spring-boot:run

# Environment variables (optional):
# POSTGRES_HOST=localhost
# POSTGRES_PORT=5432
# POSTGRES_DB=transactions
# POSTGRES_USER=transactionuser
# POSTGRES_PASSWORD=transactionpass
```

### 4. Verify Services

```bash
# Check health endpoints
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8081/actuator/health  # Fraud Detection
curl http://localhost:8082/actuator/health  # Analytics
curl http://localhost:8083/actuator/health  # Payment Processor
```

### 5. Test with Single Transaction

```bash
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user_123",
    "amount": 149.99,
    "currency": "USD",
    "merchant": "Amazon",
    "merchantCategory": "RETAIL",
    "type": "PAYMENT",
    "location": {
      "country": "US",
      "city": "New York"
    },
    "timestamp": "2026-02-07T12:00:00Z"
  }'
```

### 6. Run Load Test

**Basic Load Test (1000 RPS for 60 seconds):**
```bash
java -jar load-testing/target/load-tester.jar \
  --url http://localhost:8080/api/v1/transactions \
  --rps 1000 \
  --duration 60 \
  --warmup 10
```

**Progressive Load Test (Ramp-up):**
```bash
java -jar load-testing/target/load-tester.jar \
  --url http://localhost:8080/api/v1/transactions \
  --progressive \
  --stages "1000:30,5000:60,10000:60,30000:30"

# Stages format: RPS:DURATION_SECONDS
# Example: Start with 1k RPS for 30s, then 5k for 60s, etc.
```

**High Load Stress Test:**
```bash
java -jar load-testing/target/load-tester.jar \
  --url http://localhost:8080/api/v1/transactions \
  --rps 50000 \
  --duration 300
```

**Load Test Options:**
```
Options:
  -u, --url <URL>          Target URL (default: http://localhost:8080/api/v1/transactions)
  -r, --rps <RPS>          Target requests per second (default: 1000)
  -d, --duration <SEC>     Test duration in seconds (default: 60)
  -w, --warmup <SEC>       Warmup duration in seconds (default: 10)
  -p, --progressive        Enable progressive load (ramp-up)
  -s, --stages <STAGES>    Progressive stages: rps1:duration1,rps2:duration2,...
  -h, --help               Show help
```

## 📊 Monitoring

### Prometheus Metrics

All services expose Prometheus metrics at `/actuator/prometheus`:

```bash
# Gateway metrics
curl http://localhost:8080/actuator/prometheus

# Fraud Detection metrics
curl http://localhost:8081/actuator/prometheus

# Analytics metrics
curl http://localhost:8082/actuator/prometheus

# Payment Processor metrics
curl http://localhost:8083/actuator/prometheus
```

### Database Monitoring

```bash
# Connect to PostgreSQL
docker exec -it postgres psql -U transactionuser -d transactions

# Query transaction count
SELECT COUNT(*) FROM transactions;

# Check inserts per second (run multiple times)
SELECT
  datname,
  xact_commit,
  tup_inserted
FROM pg_stat_database
WHERE datname = 'transactions';

# Active connections
SELECT count(*) FROM pg_stat_activity;

# Table size
SELECT pg_size_pretty(pg_total_relation_size('transactions'));
```

### Redis Monitoring

```bash
# Connect to Redis
docker exec -it redis redis-cli

# Check rate limit keys
KEYS ratelimit:*

# Check analytics keys
KEYS analytics:*

# Memory usage
INFO memory
```

## 🐳 Docker Build

Build Docker images for all services:

```bash
# Build gateway-service
cd services/gateway-service
docker build -t <your-dockerhub>/gateway-service:latest .

# Repeat for other services...
```

## ☸️ Kubernetes Deployment

### Prerequisites

- Kubernetes cluster (k3s recommended for lightweight setup)
- kubectl configured
- Helm (optional, for Prometheus/Grafana)

### Deploy to K8s

```bash
# Create namespace
kubectl create namespace transaction-system

# Apply all manifests
kubectl apply -f k8s/

# Check pods
kubectl get pods -n transaction-system

# Check HPA status
kubectl get hpa -n transaction-system --watch

# Port-forward to gateway
kubectl port-forward -n transaction-system svc/gateway-service 8080:8080

# Run load test against K8s
java -jar load-testing/target/load-tester.jar \
  --url http://localhost:8080/api/v1/transactions \
  --rps 30000 \
  --duration 600 \
  --progressive \
  --stages "5000:60,10000:120,30000:180,50000:120"
```

### Monitor K8s

```bash
# Watch HPA autoscaling
kubectl get hpa -n transaction-system --watch

# Watch pod metrics
kubectl top pods -n transaction-system

# Watch events
kubectl get events -n transaction-system --watch --sort-by='.lastTimestamp'

# Check logs
kubectl logs -f <pod-name> -n transaction-system
```

## 📁 Project Structure

```
k8s-config-java/
├── shared-models/              # Shared DTOs (Transaction, FraudAlert, etc.)
├── services/
│   ├── gateway-service/        # REST API Producer
│   ├── fraud-detection-service/# Fraud analysis consumer
│   ├── analytics-service/      # Real-time analytics consumer
│   └── payment-processor-service/# Payment processing + PostgreSQL
├── load-testing/               # Virtual Threads load tester
├── k8s/                        # Kubernetes manifests
│   ├── namespaces/
│   ├── configmaps/
│   ├── secrets/
│   ├── deployments/
│   ├── services/
│   ├── hpa/
│   ├── pdb/
│   └── statefulsets/
├── docker-compose.yml          # Local development stack
├── pom.xml                     # Root Maven POM
└── README.md
```

## 🛠️ Tech Stack

### Backend
- Java 21 (Virtual Threads)
- Spring Boot 3.2+
- Spring Kafka
- Spring Data JPA + Hibernate
- Spring Data Redis
- HikariCP (connection pooling)
- Micrometer + Prometheus

### Infrastructure
- Kubernetes (k3s)
- Kafka 3.6+
- Redis 7+
- PostgreSQL 15+
- Prometheus + Grafana

### Load Testing
- Java 21 Virtual Threads
- Apache HttpClient5
- Custom rate limiting engine

## 📝 Configuration

### Environment Variables

**Gateway Service:**
```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
REDIS_HOST=localhost
REDIS_PORT=6379
```

**Payment Processor Service:**
```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=transactions
POSTGRES_USER=transactionuser
POSTGRES_PASSWORD=transactionpass
```

## 🔧 Troubleshooting

### Services won't start

```bash
# Check if infrastructure is running
docker-compose ps

# Check Kafka topics
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check PostgreSQL connection
docker exec -it postgres psql -U transactionuser -d transactions -c "SELECT 1"
```

### High CPU/Memory usage

```bash
# Check JVM heap settings
java -XX:+PrintFlagsFinal -version | grep HeapSize

# Run with custom heap (example: 2GB)
java -Xmx2g -Xms2g -jar services/gateway-service/target/gateway-service-1.0.0.jar
```

### Kafka consumer lag

```bash
# Check consumer group lag
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group payment-processor-group \
  --describe
```

## 📚 Further Reading

- [PROJECT_PLAN.md](PROJECT_PLAN.md) - Detailed architecture and K8s practice scenarios
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Java 21 Virtual Threads](https://openjdk.org/jeps/444)

## 📄 License

MIT License - feel free to use for learning and practice!

---

**Author:** Pet project for K8s practice under high load
**Last Updated:** 2026-02-07
