# Weather Forecast Service Helm Deployment

This directory contains the Helm chart for deploying the Weather Forecast Service to Kubernetes, following the same pattern as the Travel Warnings service.

## Quick Start

### Development Installation

```bash
cd /Users/dennishoang/Documents/se/cad-backend/weather-forecast-service

helm install weather-forecast-service ./helm/weather-forecast-service \
  --values ./helm/weather-forecast-service/values-dev.yaml \
  --set meteosource.apiKey=YOUR_API_KEY
```

### Production Installation (GCP)

```bash
helm install weather-forecast-service ./helm/weather-forecast-service \
  --values ./helm/weather-forecast-service/values-prod.yaml \
  --set meteosource.apiKey=YOUR_PRODUCTION_API_KEY
```

## Using the Makefile

You can also use the provided Makefile for common operations:

```bash
# Lint the chart
make lint

# Install development
make install

# Install production
make install-prod METEOSOURCE_API_KEY=your_key

# Upgrade
make upgrade

# View status
make status

# View logs
make logs

# Uninstall
make uninstall
```

## Key Differences from kubectl apply

The Helm chart uses the same structure as `travel-warnings-chart`:

1. **Structured values files**:
   - `values.yaml` - Default values
   - `values-dev.yaml` - Development overrides
   - `values-prod.yaml` - Production overrides (GCP image)

2. **Conditional templates**:
   - `ingress-dev.yaml` - No TLS for development
   - `ingress-prod.yaml` - TLS with cert-manager for production

3. **Templated resources**:
   - All values are configurable via `--set` or values files
   - Easy environment switching

4. **Production image**: Uses GCP Artifact Registry
   ```
   europe-west1-docker.pkg.dev/graphite-plane-474510-s9/docker-repo/weather-forecast-service:latest
   ```

## File Structure

```
helm/weather-forecast-service/
├── Chart.yaml                        # Chart metadata
├── values.yaml                       # Default values
├── values-dev.yaml                   # Development values
├── values-prod.yaml                  # Production values (GCP)
├── .helmignore                       # Files to ignore
├── README.md                         # Detailed documentation
└── templates/
    ├── weather-forecast-api.yaml     # Deployment & Service
    ├── postgres.yaml                 # PostgreSQL Deployment & Service
    ├── postgres-pvc.yaml             # PersistentVolumeClaim
    ├── app-config.yaml               # ConfigMap
    ├── meteosource-secret.yaml       # Secret for API key
    ├── ingress-dev.yaml              # Development ingress
    ├── ingress-prod.yaml             # Production ingress with TLS
    └── certificate.yaml              # cert-manager Certificate
```

## Testing

```bash
# Validate chart
helm lint ./helm/weather-forecast-service

# Dry-run
helm install weather-forecast-service ./helm/weather-forecast-service \
  --values ./helm/weather-forecast-service/values-prod.yaml \
  --set meteosource.apiKey=test \
  --dry-run --debug

# Template (see generated YAML)
helm template weather-forecast-service ./helm/weather-forecast-service \
  --values ./helm/weather-forecast-service/values-prod.yaml \
  --set meteosource.apiKey=test
```

## Migration from k8s/ Directory

The old Kubernetes manifests in `k8s/` are now replaced by this Helm chart:

| Old File | New Template |
|----------|--------------|
| `deployment.yaml` | `weather-forecast-api.yaml` |
| `configmap.yaml` | `app-config.yaml` |
| `secret.yaml` | `meteosource-secret.yaml` |
| `postgres-deployment.yaml` | `postgres.yaml` |
| `postgres-pvc.yaml` | `postgres-pvc.yaml` |
| `ingress-prod.yaml` | `ingress-prod.yaml` + `certificate.yaml` |

## Troubleshooting

See the detailed README in `./weather-forecast-service/README.md` for troubleshooting tips.
