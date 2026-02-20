# 🚀 Deployment Guide

Пошаговая инструкция для деплоя Transaction Processing System на сервер.

## 📋 Требования

- VPS/сервер с Ubuntu 20.04+ (минимум 4GB RAM, 2 CPU)
- DockerHub аккаунт
- GitHub аккаунт

---

## Шаг 1: Настройка GitHub Secrets

1. Зайди в свой репозиторий на GitHub
2. Settings → Secrets and variables → Actions → New repository secret

Добавь 2 секрета:

- `DOCKERHUB_USERNAME` = твой DockerHub username
- `DOCKERHUB_TOKEN` = [создай токен тут](https://hub.docker.com/settings/security)

---

## Шаг 2: Обновить Docker образы в манифестах

В файлах `k8s/deployments/*.yaml` замени `YOUR_DOCKERHUB_USERNAME` на свой DockerHub username:

```yaml
image: YOUR_DOCKERHUB_USERNAME/gateway-service:latest
```

Поменяй на:

```yaml
image: твой-username/gateway-service:latest
```

Сделай это для всех 4 файлов:
- `k8s/deployments/gateway-service.yaml`
- `k8s/deployments/fraud-detection-service.yaml`
- `k8s/deployments/analytics-service.yaml`
- `k8s/deployments/payment-processor-service.yaml`

---

## Шаг 3: Закоммитить и запушить

```bash
git add .
git commit -m "Configure Docker images for deployment"
git push origin main
```

Это автоматически запустит GitHub Actions, который:
✅ Соберёт Maven проект
✅ Создаст Docker образы
✅ Запушит их в DockerHub

Проверь статус: GitHub → Actions → должен быть зелёный ✓

---

## Шаг 4: Подготовка сервера

SSH на свой сервер:

```bash
ssh root@your-server-ip
```

### 4.1 Установить k3s (Kubernetes)

```bash
curl -sfL https://get.k3s.io | sh -

# Проверить что работает
kubectl get nodes
```

### 4.2 Установить git

```bash
apt update && apt install git -y
```

---

## Шаг 5: Клонировать проект на сервер

```bash
cd /opt
git clone https://github.com/your-username/k8s-config-java.git
cd k8s-config-java
```

---

## Шаг 6: Деплой инфраструктуры (Zookeeper, Kafka, Redis, PostgreSQL)

```bash
# Создать namespace
kubectl create namespace transaction-system

# Деплой Zookeeper (нужен для Kafka)
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: zookeeper
  namespace: transaction-system
spec:
  serviceName: zookeeper
  replicas: 1
  selector:
    matchLabels:
      app: zookeeper
  template:
    metadata:
      labels:
        app: zookeeper
    spec:
      containers:
      - name: zookeeper
        image: confluentinc/cp-zookeeper:7.5.0
        ports:
        - containerPort: 2181
        env:
        - name: ZOOKEEPER_CLIENT_PORT
          value: "2181"
        - name: ZOOKEEPER_TICK_TIME
          value: "2000"
---
apiVersion: v1
kind: Service
metadata:
  name: zookeeper
  namespace: transaction-system
spec:
  ports:
  - port: 2181
  selector:
    app: zookeeper
EOF

# Деплой Kafka
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: kafka
  namespace: transaction-system
spec:
  serviceName: kafka
  replicas: 1
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
    spec:
      containers:
      - name: kafka
        image: confluentinc/cp-kafka:7.5.0
        ports:
        - containerPort: 9092
        env:
        - name: KAFKA_BROKER_ID
          value: "1"
        - name: KAFKA_ZOOKEEPER_CONNECT
          value: "zookeeper:2181"
        - name: KAFKA_ADVERTISED_LISTENERS
          value: "PLAINTEXT://kafka:9092"
        - name: KAFKA_LISTENERS
          value: "PLAINTEXT://0.0.0.0:9092"
        - name: KAFKA_LISTENER_SECURITY_PROTOCOL_MAP
          value: "PLAINTEXT:PLAINTEXT"
        - name: KAFKA_INTER_BROKER_LISTENER_NAME
          value: "PLAINTEXT"
        - name: KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
          value: "1"
        - name: KAFKA_AUTO_CREATE_TOPICS_ENABLE
          value: "true"
---
apiVersion: v1
kind: Service
metadata:
  name: kafka
  namespace: transaction-system
spec:
  ports:
  - port: 9092
  clusterIP: None
  selector:
    app: kafka
EOF

# Деплой Redis
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: transaction-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379
---
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: transaction-system
spec:
  ports:
  - port: 6379
  selector:
    app: redis
EOF

# Деплой PostgreSQL
kubectl apply -f - <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: postgres-secret
  namespace: transaction-system
type: Opaque
stringData:
  password: transactionpass
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: transaction-system
spec:
  serviceName: postgres
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: transactions
        - name: POSTGRES_USER
          value: transactionuser
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: password
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: transaction-system
spec:
  ports:
  - port: 5432
  selector:
    app: postgres
EOF
```

---

## Шаг 7: Деплой микросервисов

```bash
# Применить все манифесты
kubectl apply -f k8s/

# Проверить статус
kubectl get pods -n transaction-system
kubectl get svc -n transaction-system
```

Ожидай пока все поды станут `Running` (может занять 2-3 минуты).

---

## Шаг 8: Установка Kubernetes Dashboard (опционально)

```bash
# Установить Dashboard
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml

# Создать admin user
kubectl apply -f - <<EOF
apiVersion: v1
kind: ServiceAccount
metadata:
  name: admin-user
  namespace: kubernetes-dashboard
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: admin-user
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-admin
subjects:
- kind: ServiceAccount
  name: admin-user
  namespace: kubernetes-dashboard
EOF

# Получить токен для входа
kubectl -n kubernetes-dashboard create token admin-user

# Запустить proxy для доступа
kubectl proxy --address='0.0.0.0' --accept-hosts='.*' &
```

Открой в браузере:
```
http://your-server-ip:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
```

Вставь токен который получил выше.

---

## Шаг 9: Проверка работы

```bash
# Получить IP сервиса
kubectl get svc gateway-service -n transaction-system

# Тестовый запрос
curl http://<EXTERNAL-IP>:8080/api/v1/transactions/health
```

Должен вернуть:
```json
{"status":"UP","service":"gateway-service"}
```

---

## 🔄 Обновление при изменениях

После изменений кода:

```bash
# На локальном компе
git add .
git commit -m "Update services"
git push origin main

# GitHub Actions автоматически пересоберёт образы

# На сервере
cd /opt/k8s-config-java
git pull
kubectl rollout restart deployment -n transaction-system
```

---

## 📊 Мониторинг

```bash
# Логи gateway сервиса
kubectl logs -f -n transaction-system -l app=gateway-service

# Статус подов
kubectl get pods -n transaction-system --watch

# Описание пода (если проблемы)
kubectl describe pod <pod-name> -n transaction-system
```

---

## 🛑 Удаление

```bash
kubectl delete namespace transaction-system
```

---

## 📝 Следующие шаги

- [ ] Настроить Ingress для доступа по доменному имени
- [ ] Добавить SSL сертификаты (Let's Encrypt)
- [ ] Настроить Prometheus + Grafana для мониторинга
- [ ] Настроить HPA (Horizontal Pod Autoscaler)
- [ ] Настроить CI/CD для автоматического деплоя на сервер
