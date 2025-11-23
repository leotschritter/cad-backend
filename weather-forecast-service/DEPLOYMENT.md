# Weather Forecast Service - Helm Deployment Guide

## Overview

The weather-forecast-service has been migrated from plain Kubernetes manifests (`k8s/` directory) to a Helm chart, following the same pattern as the `travel-warnings` service.

## Key Features

✅ **Environment-specific configurations**: Development and Production values
✅ **GCP Artifact Registry**: Production uses GCP-hosted images
✅ **Conditional templates**: Different ingress for dev/prod
✅ **TLS support**: cert-manager integration for production
✅ **Easy deployment**: Makefile with common operations
✅ **Validated**: Helm lint passes successfully

## Quick Deployment

### Development

```bash
make install-dev METEOSOURCE_API_KEY=your_dev_key
```

### Production (GCP)

```bash
make install-prod METEOSOURCE_API_KEY=your_production_key
```

## Image Configuration

- **Development**: `ghcr.io/leotschritter/weather-forecast-service:latest`
- **Production**: `europe-west1-docker.pkg.dev/graphite-plane-474510-s9/docker-repo/weather-forecast-service:latest`

The production image is automatically set in `values-prod.yaml`.

## Architecture

```
weather-forecast-service (Helm Chart)
│
├── Weather Forecast API (2 replicas in prod)
│   ├── Quarkus application
│   ├── Health probes (startup, readiness, liveness)
│   └── Resource limits (512Mi-1Gi memory, 250m-1000m CPU)
│
├── PostgreSQL (1 replica)
│   ├── postgres:15-alpine
│   ├── PersistentVolumeClaim (5Gi)
│   └── Database: weather_forecast
│
├── Configuration
│   ├── ConfigMap (app-config.yaml)
│   │   ├── Database URL
│   │   └── Meteosource API settings
│   └── Secret (meteosource-secret.yaml)
│       └── API Key
│
└── Networking
    ├── ClusterIP Services
    ├── Ingress (dev or prod with TLS)
    └── Certificate (cert-manager in prod)
```

## Migration from k8s/ Directory

### Old Structure (kubectl apply)

```
k8s/
├── deployment.yaml           # App deployment + service
├── configmap.yaml            # Configuration
├── secret.yaml               # Secrets
├── postgres-deployment.yaml  # PostgreSQL
├── postgres-pvc.yaml         # Storage
└── ingress-prod.yaml         # Ingress
```

### New Structure (Helm)

```
helm/weather-forecast-service/
├── Chart.yaml
├── values.yaml               # Base values
├── values-dev.yaml           # Dev overrides
├── values-prod.yaml          # Prod overrides (GCP image!)
└── templates/
    ├── weather-forecast-api.yaml
    ├── postgres.yaml
    ├── postgres-pvc.yaml
    ├── app-config.yaml
    ├── meteosource-secret.yaml
    ├── ingress-dev.yaml
    ├── ingress-prod.yaml
    └── certificate.yaml
```

## Key Differences

| Aspect | Old (k8s/) | New (Helm) |
|--------|-----------|------------|
| **Deployment** | `kubectl apply -f k8s/` | `helm install` or `make install-prod` |
| **Configuration** | Manual file editing | Values files + `--set` flags |
| **Environments** | Same manifests for all | Separate values-dev.yaml, values-prod.yaml |
| **Image Management** | Hardcoded in YAML | Configurable via values |
| **TLS** | Always enabled | Conditional (prod only) |
| **Updates** | `kubectl apply` | `helm upgrade` |
| **Rollback** | Manual | `helm rollback` |

## Deployment Commands

### Installation

```bash
# Development
make install-dev METEOSOURCE_API_KEY=your_key

# Production
make install-prod METEOSOURCE_API_KEY=your_key
```

### Upgrade

```bash
# Upgrade production
make upgrade-prod METEOSOURCE_API_KEY=your_key

# Upgrade with current values (no changes)
make upgrade
```

### Verification

```bash
# Check status
make status

# View logs
make logs

# View PostgreSQL logs
make logs-db
```

### Uninstall

```bash
# Uninstall (keeps PVC)
make uninstall

# Complete cleanup (deletes database!)
make clean
```

## Configuration

### Required Parameters

- `meteosource.apiKey` - **REQUIRED**: Meteosource API key

### Optional Overrides

```bash
# Custom replica count
--set weatherForecastApi.replicas=3

# Custom PostgreSQL password
--set postgres.environmentVariables.password=newpassword

# Custom storage size
--set postgresPvc.storage=10Gi
```

## Health Checks

The application includes three types of probes:

1. **Startup Probe** (`/q/health/started`)
   - Checks if application has started
   - Allows 150 seconds for startup (30 failures × 5 seconds)

2. **Readiness Probe** (`/q/health/ready`)
   - Checks if application is ready to serve traffic
   - Checked every 10 seconds

3. **Liveness Probe** (`/q/health/live`)
   - Checks if application is still alive
   - Restarts container if failing

## Troubleshooting

### Chart validation

```bash
make lint
```

### Dry-run test

```bash
make test-dry-run METEOSOURCE_API_KEY=test
```

### View generated templates

```bash
make template METEOSOURCE_API_KEY=test
```

### Check pod logs

```bash
kubectl logs -l app=weather-forecast-service
```

### Check PostgreSQL

```bash
kubectl get pods -l app=weather-postgres
kubectl logs -l app=weather-postgres
```

## CI/CD Integration

The chart is ready for CI/CD pipelines:

```bash
# In your CI/CD pipeline
helm upgrade --install weather-forecast-service ./helm/weather-forecast-service \
  --values ./helm/weather-forecast-service/values-prod.yaml \
  --set meteosource.apiKey=$METEOSOURCE_API_KEY \
  --wait \
  --timeout 5m
```

## Next Steps

1. **Delete old k8s/ manifests** (after successful migration)
2. **Update CI/CD pipelines** to use Helm
3. **Configure monitoring** for the new deployment
4. **Set up automated backups** for PostgreSQL PVC

## Support

For issues or questions:
1. Check logs: `make logs`
2. Validate chart: `make lint`
3. Review templates: `make template`
4. Consult README: `helm/weather-forecast-service/README.md`
