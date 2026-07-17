# Convenience targets. Run `make help` to list them.

.PHONY: help build test up down logs clean

help:  ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}'

build:  ## Compile and package every module
	mvn -B clean package -DskipTests

test:  ## Run the test suite
	mvn -B test

up:  ## Start the whole stack with Docker Compose
	docker compose up --build -d
	@echo "Gateway: http://localhost:8080   Eureka: http://localhost:8761"

down:  ## Stop the stack and drop the volumes
	docker compose down -v

logs:  ## Tail the logs of all services
	docker compose logs -f

clean:  ## Remove build output
	mvn -B clean
