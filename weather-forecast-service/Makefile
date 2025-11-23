.PHONY: help lint test-dry-run install install-dev install-prod upgrade upgrade-prod uninstall status logs logs-db clean template

CHART_NAME := weather-forecast-service
CHART_PATH := ./helm/weather-forecast-service
NAMESPACE := default
RELEASE_NAME := weather-forecast-service

# Set via command line: make install-prod METEOSOURCE_API_KEY=your_key
METEOSOURCE_API_KEY ?=

help:
	@echo "Weather Forecast Service - Helm Deployment"
	@echo ""
	@echo "Available targets:"
	@echo "  lint              - Validate Helm chart"
	@echo "  test-dry-run      - Test production installation without deploying"
	@echo "  template          - Render templates locally"
	@echo "  install-dev       - Install with development values"
	@echo "  install-prod      - Install with production values (GCP image)"
	@echo "  upgrade           - Upgrade with current values"
	@echo "  upgrade-prod      - Upgrade with production values"
	@echo "  uninstall         - Uninstall chart"
	@echo "  status            - Show installation status"
	@echo "  logs              - View application logs"
	@echo "  logs-db           - View database logs"
	@echo "  clean             - Delete all resources including PVC"
	@echo ""
	@echo "Usage examples:"
	@echo "  make install-dev METEOSOURCE_API_KEY=your_key"
	@echo "  make install-prod METEOSOURCE_API_KEY=your_prod_key"
	@echo "  make upgrade-prod METEOSOURCE_API_KEY=your_key"

lint:
	@echo "Linting Helm chart..."
	helm lint $(CHART_PATH)

test-dry-run:
	@echo "Testing Helm installation (dry-run)..."
	@if [ -z "$(METEOSOURCE_API_KEY)" ]; then \
		echo "ERROR: METEOSOURCE_API_KEY not set!"; \
		echo "Usage: make test-dry-run METEOSOURCE_API_KEY=your_key"; \
		exit 1; \
	fi
	helm install $(RELEASE_NAME) $(CHART_PATH) \
		--values $(CHART_PATH)/values-prod.yaml \
		--set meteosource.apiKey=$(METEOSOURCE_API_KEY) \
		--dry-run=client --debug | head -100

template:
	@echo "Rendering Helm templates..."
	@if [ -z "$(METEOSOURCE_API_KEY)" ]; then \
		echo "WARNING: METEOSOURCE_API_KEY not set, using 'test' as placeholder"; \
		METEOSOURCE_API_KEY=test; \
	fi
	helm template $(RELEASE_NAME) $(CHART_PATH) \
		--values $(CHART_PATH)/values-prod.yaml \
		--set meteosource.apiKey=$${METEOSOURCE_API_KEY}

install-dev:
	@echo "Installing $(CHART_NAME) with development values..."
	@if [ -z "$(METEOSOURCE_API_KEY)" ]; then \
		echo "ERROR: METEOSOURCE_API_KEY not set!"; \
		echo "Usage: make install-dev METEOSOURCE_API_KEY=your_key"; \
		exit 1; \
	fi
	helm install $(RELEASE_NAME) $(CHART_PATH) \
		--values $(CHART_PATH)/values-dev.yaml \
		--set meteosource.apiKey=$(METEOSOURCE_API_KEY) \
		--namespace $(NAMESPACE) \
		--create-namespace
	@echo ""
	@echo "Development installation complete! Check status with: make status"

install-prod:
	@echo "Installing $(CHART_NAME) with production values (GCP image)..."
	@if [ -z "$(METEOSOURCE_API_KEY)" ]; then \
		echo "ERROR: METEOSOURCE_API_KEY not set!"; \
		echo "Usage: make install-prod METEOSOURCE_API_KEY=your_prod_key"; \
		exit 1; \
	fi
	helm install $(RELEASE_NAME) $(CHART_PATH) \
		--values $(CHART_PATH)/values-prod.yaml \
		--set meteosource.apiKey=$(METEOSOURCE_API_KEY) \
		--namespace $(NAMESPACE) \
		--create-namespace
	@echo ""
	@echo "Production installation complete! Check status with: make status"
	@echo "Image: europe-west1-docker.pkg.dev/graphite-plane-474510-s9/docker-repo/weather-forecast-service:latest"

upgrade:
	@echo "Upgrading $(CHART_NAME) with current values..."
	helm upgrade $(RELEASE_NAME) $(CHART_PATH) \
		--namespace $(NAMESPACE) \
		--reuse-values
	@echo ""
	@echo "Upgrade complete!"

upgrade-prod:
	@echo "Upgrading $(CHART_NAME) with production values..."
	@if [ -z "$(METEOSOURCE_API_KEY)" ]; then \
		echo "ERROR: METEOSOURCE_API_KEY not set!"; \
		echo "Usage: make upgrade-prod METEOSOURCE_API_KEY=your_key"; \
		exit 1; \
	fi
	helm upgrade $(RELEASE_NAME) $(CHART_PATH) \
		--values $(CHART_PATH)/values-prod.yaml \
		--set meteosource.apiKey=$(METEOSOURCE_API_KEY) \
		--namespace $(NAMESPACE)
	@echo ""
	@echo "Production upgrade complete!"

uninstall:
	@echo "Uninstalling $(CHART_NAME)..."
	helm uninstall $(RELEASE_NAME) --namespace $(NAMESPACE)
	@echo ""
	@echo "Uninstallation complete!"

status:
	@echo "Helm release status:"
	helm status $(RELEASE_NAME) --namespace $(NAMESPACE)
	@echo ""
	@echo "Kubernetes resources:"
	kubectl get all -l app=$(CHART_NAME) --namespace $(NAMESPACE)
	@echo ""
	@echo "PostgreSQL:"
	kubectl get pods,svc,pvc -l app=weather-postgres --namespace $(NAMESPACE)

logs:
	@echo "Streaming application logs (Ctrl+C to stop)..."
	kubectl logs -l app=$(CHART_NAME) -f --namespace $(NAMESPACE)

logs-db:
	@echo "Streaming PostgreSQL logs (Ctrl+C to stop)..."
	kubectl logs -l app=weather-postgres -f --namespace $(NAMESPACE)

clean:
	@echo "WARNING: This will delete all resources including the database PVC!"
	@echo "Press Ctrl+C to cancel, or Enter to continue..."
	@read dummy
	$(MAKE) uninstall
	kubectl delete pvc weather-postgres-data-pvc --namespace $(NAMESPACE) --ignore-not-found
	@echo "Cleanup complete!"

# Development helpers
dev: lint install-dev

# Production helpers
prod: lint install-prod
