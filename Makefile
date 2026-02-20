# Makefile for Transaction Processing System

# Variables
DOCKERHUB_USERNAME ?= yourusername
VERSION ?= latest
SERVICES = gateway-service fraud-detection-service analytics-service payment-processor-service

# Colors for output
GREEN  := $(shell tput -Txterm setaf 2)
YELLOW := $(shell tput -Txterm setaf 3)
RESET  := $(shell tput -Txterm sgr0)

.PHONY: help
help: ## Show this help
	@echo "$(GREEN)Available targets:$(RESET)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(YELLOW)%-20s$(RESET) %s\n", $$1, $$2}'

.PHONY: build
build: ## Build all services with Maven
	@echo "$(GREEN)Building all services...$(RESET)"
	mvn clean install -DskipTests

.PHONY: test
test: ## Run all tests
	@echo "$(GREEN)Running tests...$(RESET)"
	mvn test

.PHONY: docker-build
docker-build: build ## Build all Docker images
	@echo "$(GREEN)Building Docker images...$(RESET)"
	@for service in $(SERVICES); do \
		echo "$(YELLOW)Building $$service...$(RESET)"; \
		docker build -t $(DOCKERHUB_USERNAME)/$$service:$(VERSION) services/$$service/; \
	done
	@echo "$(GREEN)All images built successfully!$(RESET)"

.PHONY: docker-push
docker-push: ## Push all Docker images to DockerHub
	@echo "$(GREEN)Pushing Docker images to DockerHub...$(RESET)"
	@for service in $(SERVICES); do \
		echo "$(YELLOW)Pushing $$service...$(RESET)"; \
		docker push $(DOCKERHUB_USERNAME)/$$service:$(VERSION); \
	done
	@echo "$(GREEN)All images pushed successfully!$(RESET)"

.PHONY: docker-build-push
docker-build-push: docker-build docker-push ## Build and push all Docker images

.PHONY: docker-login
docker-login: ## Login to DockerHub
	@echo "$(GREEN)Logging in to DockerHub...$(RESET)"
	docker login

.PHONY: k8s-deploy
k8s-deploy: ## Deploy to Kubernetes
	@echo "$(GREEN)Deploying to Kubernetes...$(RESET)"
	kubectl apply -f k8s/

.PHONY: k8s-delete
k8s-delete: ## Delete from Kubernetes
	@echo "$(YELLOW)Deleting from Kubernetes...$(RESET)"
	kubectl delete -f k8s/

.PHONY: k8s-status
k8s-status: ## Check Kubernetes deployment status
	@echo "$(GREEN)Checking Kubernetes status...$(RESET)"
	kubectl get pods -n transaction-system
	kubectl get svc -n transaction-system
	kubectl get hpa -n transaction-system

.PHONY: k8s-logs
k8s-logs: ## Show logs from gateway service
	kubectl logs -f -n transaction-system -l app=gateway-service

.PHONY: local-infra
local-infra: ## Start local infrastructure (Kafka, Redis, PostgreSQL)
	@echo "$(GREEN)Starting local infrastructure...$(RESET)"
	docker-compose up -d

.PHONY: local-infra-down
local-infra-down: ## Stop local infrastructure
	@echo "$(YELLOW)Stopping local infrastructure...$(RESET)"
	docker-compose down

.PHONY: clean
clean: ## Clean build artifacts
	@echo "$(GREEN)Cleaning build artifacts...$(RESET)"
	mvn clean

.PHONY: full-deploy
full-deploy: docker-build-push k8s-deploy ## Full pipeline: build, push, and deploy to K8s
	@echo "$(GREEN)Full deployment completed!$(RESET)"
