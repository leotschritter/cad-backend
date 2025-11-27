# Itinerary Service Helm Chart

A production-ready Helm chart for deploying the Itinerary Service (Travel Planning Application) to Kubernetes/GKE.

## Features

- ✅ **Quarkus Application**: Optimized for cloud-native deployment
- ✅ **GCP Integration**: Cloud SQL, GCS bucket support with Workload Identity
- ✅ **Auto-scaling**: Horizontal Pod Autoscaler based on CPU/memory
- ✅ **High Availability**: Multiple replicas with PodDisruptionBudget
- ✅ **TLS/SSL**: Automatic certificate management via cert-manager
- ✅ **Health Checks**: Startup, liveness, and readiness probes
- ✅ **ConfigMap/Secret Management**: Secure credential handling
- ✅ **Identity Platform**: Firebase Authentication integration

## Prerequisites

- Kubernetes 1.21+
- Helm 3.x
- kubectl configured to access your cluster
- cert-manager installed (for TLS certificates)
- NGINX Ingress Controller
- GCP Workload Identity configured (for GKE)

## Installation

### Quick Start (Development)

```bash
helm install itinerary-service ./kubernetes/itinerary-service-chart \
  --namespace default \
  --set database.password=YOUR_DB_PASSWORD \
  -f ./kubernetes/itinerary-service-chart/values-dev.yaml
```

### Production Deployment

```bash
helm install itinerary-service ./kubernetes/itinerary-service-chart \
  --namespace default \
  --set database.password=YOUR_DB_PASSWORD \
  --set image.tag=2.0.2 \
  -f ./kubernetes/itinerary-service-chart/values-prod.yaml \
  --wait --timeout 5m
```

### CI/CD Deployment (GitHub Actions)

```bash
helm upgrade --install itinerary-service ./kubernetes/itinerary-service-chart \
  --namespace default \
  --set database.password=${{ secrets.DB_PASSWORD }} \
  --set image.tag=${{ github.sha }} \
  -f ./kubernetes/itinerary-service-chart/values-prod.yaml \
  --wait --timeout 5m
```

## Configuration

### Key Values

| Parameter | Description | Default |
|-----------|-------------|---------|
| `replicaCount` | Number of replicas | `1` |
| `image.repository` | Docker image repository | `europe-west1-docker.pkg.dev/...` |
| `image.tag` | Docker image tag | `2.0.2` |
| `image.pullPolicy` | Image pull policy | `Always` |
| `database.user` | Database username | `cad_db_user` |
| `database.password` | Database password (required) | `""` |
| `database.url` | JDBC connection URL | Cloud SQL proxy URL |
| `gcp.projectId` | GCP project ID | `graphite-plane-474510-s9` |
| `gcp.bucketName` | GCS bucket name | `graphite-plane-474510-s9-tripico-images` |
| `ingress.enabled` | Enable ingress | `true` |
| `certificate.enabled` | Enable cert-manager certificate | `true` |
| `autoscaling.enabled` | Enable HPA | `true` |
| `autoscaling.minReplicas` | Minimum replicas | `1` |
| `autoscaling.maxReplicas` | Maximum replicas | `10` |

### Custom Values File

Create a `custom-values.yaml`:

```yaml
replicaCount: 3

image:
  tag: "v2.1.0"

database:
  password: "my-secret-password"

resources:
  limits:
    cpu: 2000m
    memory: 2Gi
  requests:
    cpu: 1000m
    memory: 1Gi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 20
  targetCPUUtilizationPercentage: 60
```

Install with custom values:

```bash
helm install itinerary-service ./kubernetes/itinerary-service-chart \
  -f custom-values.yaml \
  --namespace default
```

## Upgrade

```bash
helm upgrade itinerary-service ./kubernetes/itinerary-service-chart \
  --namespace default \
  --set database.password=YOUR_DB_PASSWORD \
  --set image.tag=NEW_VERSION \
  -f ./kubernetes/itinerary-service-chart/values-prod.yaml \
  --wait --timeout 5m
```

## Rollback

```bash
# View release history
helm history itinerary-service -n default

# Rollback to previous version
helm rollback itinerary-service -n default

# Rollback to specific revision
helm rollback itinerary-service 3 -n default
```

## Uninstall

```bash
helm uninstall itinerary-service --namespace default
```

## Architecture

### Components

- **Deployment**: Manages application pods with rolling updates
- **Service**: ClusterIP service for internal communication
- **Ingress**: NGINX ingress with TLS termination
- **Certificate**: cert-manager certificate for automatic TLS
- **HorizontalPodAutoscaler**: Scales pods based on metrics
- **ConfigMap**: Application configuration
- **Secret**: Sensitive credentials (database password)
- **ServiceAccount**: GCP Workload Identity binding
- **PodDisruptionBudget**: Ensures high availability during updates

### GCP Integration

The chart uses **GCP Workload Identity** to securely access:
- Cloud SQL (PostgreSQL)
- Google Cloud Storage
- Identity Platform (Firebase Auth)

The service account annotation binds the Kubernetes SA to the GCP service account:
```yaml
iam.gke.io/gcp-service-account: "travel-backend-sa@graphite-plane-474510-s9.iam.gserviceaccount.com"
```

## Health Checks

The application exposes Quarkus health endpoints:

- **Startup**: `/q/health/started` - Checks if app has started
- **Liveness**: `/q/health/live` - Checks if app is alive
- **Readiness**: `/q/health/ready` - Checks if app can accept traffic

You can test them:

```bash
# Via port-forward
kubectl port-forward svc/itinerary-service 8080:8080
curl http://localhost:8080/q/health

# Via ingress
curl https://itinerary.tripico.fun/q/health
```

## Monitoring

### View Logs

```bash
# All pods
kubectl logs -l app=itinerary-service --tail=100 -f

# Specific pod
kubectl logs -f deployment/itinerary-service
```

### Check Status

```bash
# Deployment status
kubectl get deployment itinerary-service

# Pod status
kubectl get pods -l app=itinerary-service

# HPA status
kubectl get hpa itinerary-service

# Service endpoints
kubectl get endpoints itinerary-service

# Ingress status
kubectl get ingress itinerary-service

# Certificate status
kubectl get certificate itinerary-tls-cert
kubectl describe certificate itinerary-tls-cert
```

### Debug Pod Issues

```bash
# Describe pod
kubectl describe pod <pod-name>

# Check events
kubectl get events --sort-by='.lastTimestamp' | grep itinerary

# Execute shell in pod
kubectl exec -it <pod-name> -- /bin/sh
```

## Troubleshooting

### Database Connection Issues

1. Check database credentials:
```bash
kubectl get secret itinerary-service-db-secret -o yaml
```

2. Verify Cloud SQL proxy connection in logs:
```bash
kubectl logs -l app=itinerary-service | grep -i "database\|sql"
```

### Certificate Not Ready

```bash
# Check certificate status
kubectl describe certificate itinerary-tls-cert

# Check cert-manager logs
kubectl logs -n cert-manager deployment/cert-manager -f

# Check ClusterIssuer
kubectl get clusterissuer letsencrypt-prod
```

### Pod CrashLoopBackOff

1. Check startup probe timeout:
```bash
kubectl describe pod <pod-name> | grep -A 20 "Liveness\|Readiness\|Startup"
```

2. Increase probe timeout in values:
```yaml
probes:
  startup:
    failureThreshold: 60  # Increase from 30
```

### High Memory Usage

Adjust JVM settings via environment variables:
```yaml
env:
  - name: JAVA_OPTS
    value: "-Xms256m -Xmx768m"
```

## Security Best Practices

✅ **Implemented:**
- Secret management for database credentials
- TLS/SSL encryption via cert-manager
- GCP Workload Identity (no service account keys)
- Resource limits to prevent DoS
- Health checks for automatic recovery
- Non-root container execution (can be enabled)

🔒 **Recommendations:**
- Enable Pod Security Standards
- Use NetworkPolicies to restrict traffic
- Implement RBAC for fine-grained access
- Enable audit logging
- Use external secrets operator for sensitive data

## Development

### Local Testing with Minikube

```bash
# Start minikube
minikube start

# Install NGINX Ingress
minikube addons enable ingress

# Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.3/cert-manager.yaml

# Install chart
helm install itinerary-service ./kubernetes/itinerary-service-chart \
  --set database.password=test123 \
  -f values-dev.yaml

# Get minikube IP
minikube ip

# Add to /etc/hosts
echo "$(minikube ip) itinerary-dev.tripico.fun" | sudo tee -a /etc/hosts
```

### Template Validation

```bash
# Render templates locally
helm template itinerary-service ./kubernetes/itinerary-service-chart \
  --set database.password=test123 \
  -f values-prod.yaml

# Dry-run installation
helm install itinerary-service ./kubernetes/itinerary-service-chart \
  --dry-run --debug \
  --set database.password=test123
```

### Linting

```bash
helm lint ./kubernetes/itinerary-service-chart
```

## CI/CD Integration Example

```yaml
# .github/workflows/deploy-itinerary.yml
- name: Deploy to GKE
  run: |
    helm upgrade --install itinerary-service ./kubernetes/itinerary-service-chart \
      --namespace default \
      --set database.password=${{ secrets.DB_PASSWORD }} \
      --set image.tag=${{ github.sha }} \
      -f ./kubernetes/itinerary-service-chart/values-prod.yaml \
      --wait --timeout 5m
    
- name: Verify Deployment
  run: |
    kubectl rollout status deployment/itinerary-service
    kubectl get pods -l app=itinerary-service
```

## Support

For issues or questions:
- Check the [Troubleshooting](#troubleshooting) section
- Review Kubernetes events: `kubectl get events`
- Check application logs: `kubectl logs -l app=itinerary-service`

## License

This chart is part of the CAD Travel Application project.

