# Convenience targets. Run `make help` to list them.

.PHONY: help build test up down logs clean web-dev \
        k8s-images k8s-load-minikube k8s-up k8s-down k8s-status k8s-logs k8s-forward

help:  ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

build:  ## Compile and package every module
	mvn -B clean package -DskipTests

test:  ## Run the test suite
	mvn -B test

up:  ## Start the whole stack with Docker Compose
	docker compose up --build -d
	@echo "Client: http://localhost:3000   Gateway: http://localhost:8080   Eureka: http://localhost:8761"
	@echo "Mailpit: http://localhost:8025  RabbitMQ: http://localhost:15672"

down:  ## Stop the stack and drop the volumes
	docker compose down -v

logs:  ## Tail the logs of all services
	docker compose logs -f

web-dev:  ## Run the web client on the Vite dev server against a running stack
	cd web-client && npm install && npm run dev

clean:  ## Remove build output
	mvn -B clean

# --------------------------- Kubernetes ---------------------------
# The manifests reference local images (imagePullPolicy: IfNotPresent) and no registry.
# Docker Desktop's cluster reads the local image store directly, so `k8s-images` is all
# that is needed there; minikube keeps its own store and needs `k8s-load-minikube` too.

k8s-images:  ## Build the images Kubernetes expects (same names Compose produces)
	docker compose build

k8s-load-minikube:  ## Copy the built images into a minikube cluster
	@for svc in discovery-server api-gateway user-service catalog-service reservation-service notification-service loyalty-service web-client; do minikube image load appointment-booking-$$svc:latest; done

k8s-up:  ## Apply every manifest and wait until everything is ready
	kubectl apply -k k8s/
	kubectl -n appointment-booking rollout status statefulset/mysql-user --timeout=300s
	kubectl -n appointment-booking rollout status statefulset/rabbitmq --timeout=300s
	kubectl -n appointment-booking rollout status deployment/discovery-server --timeout=300s
	kubectl -n appointment-booking rollout status deployment/api-gateway --timeout=300s
	kubectl -n appointment-booking rollout status deployment/user-service --timeout=300s
	kubectl -n appointment-booking rollout status deployment/catalog-service --timeout=300s
	kubectl -n appointment-booking rollout status deployment/reservation-service --timeout=300s
	kubectl -n appointment-booking rollout status deployment/notification-service --timeout=300s
	kubectl -n appointment-booking rollout status deployment/loyalty-service --timeout=300s
	kubectl -n appointment-booking rollout status deployment/web-client --timeout=300s
	@echo ""
	@echo "Ready. Open http://appointment-booking.local  (needs an ingress controller and a"
	@echo "hosts entry), or run 'make k8s-forward' and open http://localhost:3000"

k8s-down:  ## Delete the namespace and everything in it, volumes included
	kubectl delete namespace appointment-booking --ignore-not-found

k8s-status:  ## Pods, services and volume claims
	kubectl -n appointment-booking get pods,svc,pvc

k8s-logs:  ## Tail the logs of every pod in the namespace
	kubectl -n appointment-booking logs -l app.kubernetes.io/part-of=appointment-booking --all-containers --tail=50 -f

# All four forwards have to run in one shell: make gives every recipe line its own
# shell, and a background process would be orphaned when that shell exits. `wait` keeps
# the shell alive so Ctrl+C stops all of them together.
k8s-forward:  ## Expose the client, gateway, Mailpit and the RabbitMQ console on localhost
	@echo "Client http://localhost:3000   Gateway http://localhost:8080"
	@echo "Mailpit http://localhost:8025  RabbitMQ http://localhost:15672   (Ctrl+C stops all)"
	@kubectl -n appointment-booking port-forward svc/web-client 3000:8080 & kubectl -n appointment-booking port-forward svc/api-gateway 8080:8080 & kubectl -n appointment-booking port-forward svc/mailpit 8025:8025 & kubectl -n appointment-booking port-forward svc/rabbitmq 15672:15672 & wait
