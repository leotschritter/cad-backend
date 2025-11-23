# Weather Forecast Service Helm Chart

This Helm chart deploys the Weather Forecast Service along with a PostgreSQL database to Kubernetes, following the same pattern as the Travel Warnings service.

## Prerequisites

- Kubernetes 1.19+
- Helm 3.0+
- cert-manager (for TLS certificates in production)
- nginx-ingress-controller

## Installation

### Development/Local Installation

```bash
# Install with development values
helm install weather-forecast-service ./helm/weather-forecast-service \
  --values ./helm/weather-forecast-service/values-dev.yaml \
  --set meteosource.apiKey=YOUR_API_KEY
```

### Production Installation on GCP

```bash
# Install with production values (uses GCP Artifact Registry image)
helm install weather-forecast-service ./helm/weather-forecast-service \
  --values ./helm/weather-forecast-service/values-prod.yaml \
  --set meteosource.apiKey=YOUR_PRODUCTION_API_KEY \
  --namespace default
```

## Configuration

### Key Values

| Parameter | Description | Default |
|-----------|-------------|---------|
| `weatherForecastApi.replicas` | Number of API replicas | `1` (dev), `2` (prod) |
| `weatherForecastApi.image` | Container image | GCP Artifact Registry (prod) |
| `postgres.replicas` | Number of PostgreSQL replicas | `1` |
| `meteosource.apiKey` | Meteosource API Key | `""` (must be set) |
| `ingress.enabled` | Enable ingress | `true` |
| `ingress.useProd` | Use production ingress with TLS | `false` (dev), `true` (prod) |

### Setting the API Key

The Meteosource API key MUST be provided during installation:

```bash
--set meteosource.apiKey=your_actual_api_key
```

### Environment-Specific Values

#### Development (`values-dev.yaml`)
- Lower resource requirements
- No TLS
- Host: `weather-forecast.local`

#### Production (`values-prod.yaml`)
- Higher availability (2 replicas)
- TLS enabled
- Host: `weather.tripico.fun`
- Uses GCP Artifact Registry image

## Upgrading

```bash
# Development
helm upgrade weather-forecast-service ./helm/weather-forecast-service \
  --values ./helm/weather-forecast-service/values-dev.yaml \
  --set meteosource.apiKey=YOUR_API_KEY

# Production
helm upgrade weather-forecast-service ./helm/weather-forecast-service \
  --values ./helm/weather-forecast-service/values-prod.yaml \
  --set meteosource.apiKey=YOUR_API_KEY
```

## Uninstalling

```bash
helm uninstall weather-forecast-service
```

**Note:** This will also delete the PersistentVolumeClaim with the database data.

## Verification

### Check deployment status

```bash
kubectl get pods -l app=weather-forecast-service
kubectl get svc weather-forecast-service
kubectl get ingress weather-forecast-ingress
```

### Check health endpoints

```bash
# Get pod name
POD_NAME=$(kubectl get pods -l app=weather-forecast-service -o jsonpath='{.items[0].metadata.name}')

# Check startup
kubectl exec -it $POD_NAME -- curl http://localhost:8080/q/health/started

# Check readiness
kubectl exec -it $POD_NAME -- curl http://localhost:8080/q/health/ready

# Check liveness
kubectl exec -it $POD_NAME -- curl http://localhost:8080/q/health/live
```

### View logs

```bash
# Application logs
kubectl logs -l app=weather-forecast-service -f

# PostgreSQL logs
kubectl logs -l app=weather-postgres -f
```

## Testing

### Validate the chart

```bash
helm lint ./helm/weather-forecast-service
```

### Dry-run installation

```bash
helm install weather-forecast-service ./helm/weather-forecast-service \
  --values ./helm/weather-forecast-service/values-prod.yaml \
  --set meteosource.apiKey=test \
  --dry-run --debug
```

### Template the chart

```bash
helm template weather-forecast-service ./helm/weather-forecast-service \
  --values ./helm/weather-forecast-service/values-prod.yaml \
  --set meteosource.apiKey=test
```

## Migration from kubectl apply

If you previously deployed using `kubectl apply -f k8s/`:

1. **Backup your data** (if needed)
2. **Delete old resources**:
   ```bash
   kubectl delete -f k8s/
   ```
3. **Install with Helm**:
   ```bash
   helm install weather-forecast-service ./helm/weather-forecast-service \
     --values ./helm/weather-forecast-service/values-prod.yaml \
     --set meteosource.apiKey=YOUR_API_KEY
   ```

## Architecture

The chart deploys:
- **Weather Forecast API**: Quarkus application (1-2 replicas)
- **PostgreSQL**: Database for storing forecasts (1 replica)
- **ConfigMap**: Application configuration
- **Secret**: Meteosource API key
- **Service**: ClusterIP services for both components
- **Ingress**: nginx ingress (dev or prod with TLS)
- **Certificate**: cert-manager certificate (prod only)
- **PVC**: Persistent storage for PostgreSQL (5Gi)

## Troubleshooting

### Pods not starting

```bash
kubectl describe pod <pod-name>
kubectl logs <pod-name>
```

### Database connection issues

Check if PostgreSQL is ready:
```bash
kubectl get pods -l app=weather-postgres
kubectl logs -l app=weather-postgres
```

### API Key not set

If you forgot to set the API key:
```bash
helm upgrade weather-forecast-service ./helm/weather-forecast-service \
  --values ./helm/weather-forecast-service/values-prod.yaml \
  --set meteosource.apiKey=YOUR_API_KEY \
  --reuse-values
```
