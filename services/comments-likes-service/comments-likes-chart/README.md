# Comments & Likes Service Helm Chart

Production-ready Helm chart for the Comments & Likes microservice - part of the Travel Planning Application.

## Overview

This is a **microservice** that handles comments and likes functionality for travel itineraries.

**Key Characteristics:**
- ✅ **Quarkus Framework** - Fast startup, low memory footprint
- ✅ **Firebase Authentication** - Secure API access
- ✅ **Auto-scaling** - HPA based on CPU (1-10 replicas)
- ✅ **TLS/SSL** - Automatic certificates via cert-manager

## Prerequisites

- Kubernetes 1.21+
- Helm 3.x
- NGINX Ingress Controller
- cert-manager (for TLS certificates)
- GCP Workload Identity configured (for GKE deployments)

## Quick Start

### Install Production

```bash
helm install comments-likes-service ./services/comments-likes-service/comments-likes-chart \
  -f ./services/comments-likes-service/comments-likes-chart/values-prod.yaml \
  --wait --timeout 5m
```

### Install Development

```bash
helm install comments-likes-service ./services/comments-likes-service/comments-likes-chart \
  -f ./services/comments-likes-service/comments-likes-chart/values-dev.yaml
```

### Upgrade Existing Release

```bash
helm upgrade comments-likes-service ./services/comments-likes-service/comments-likes-chart \
  -f ./services/comments-likes-service/comments-likes-chart/values-prod.yaml \
  --wait --timeout 5m
```

## Configuration

### Key Values

| Parameter | Description | Default |
|-----------|-------------|---------|
| `replicaCount` | Number of pod replicas | `1` |
| `image.repository` | Container image repository | `europe-west1-docker.pkg.dev/.../comments-likes-service` |
| `image.pullPolicy` | Image pull policy | `Always` |
| `service.type` | Kubernetes service type | `ClusterIP` |
| `service.port` | Service port | `8080` |
| `ingress.enabled` | Enable ingress resource | `true` |
| `ingress.hosts[0].host` | Domain name | `cl.tripico.fun` |
| `autoscaling.enabled` | Enable Horizontal Pod Autoscaler | `true` |
| `autoscaling.minReplicas` | Minimum number of replicas | `1` |
| `autoscaling.maxReplicas` | Maximum number of replicas | `10` |
| `autoscaling.targetCPUUtilizationPercentage` | CPU threshold for scaling | `75` |
| `resources.requests.cpu` | CPU request | `250m` |
| `resources.requests.memory` | Memory request | `512Mi` |
| `resources.limits.cpu` | CPU limit | `1000m` |
| `resources.limits.memory` | Memory limit | `1Gi` |

### Version Management

The Docker image version is controlled by `Chart.yaml`:

```yaml
# Chart.yaml
appVersion: "2.1.6"  # This determines the Docker image tag
```

**To deploy a new version:**
1. Update `appVersion` in `Chart.yaml`
2. Commit and push changes
3. CI/CD pipeline automatically builds and deploys

### Custom Values

Create a custom values file:

```yaml
# custom-values.yaml
replicaCount: 3

resources:
  requests:
    cpu: 500m
    memory: 768Mi
  limits:
    cpu: 1500m
    memory: 1536Mi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 15
  targetCPUUtilizationPercentage: 65
```

Apply with:

```bash
helm install comments-likes-service ./services/comments-likes-service/comments-likes-chart \
  -f custom-values.yaml
```

## Architecture

### Components

The chart deploys:

- **Deployment** - Manages stateless application pods with rolling updates
- **Service** - ClusterIP for internal communication on port 8080
- **Ingress** - NGINX ingress with TLS termination
- **HorizontalPodAutoscaler** - Auto-scales pods based on CPU usage
- **ConfigMap** - Application configuration (Firebase, GCP settings)
- **ServiceAccount** - GCP Workload Identity binding
- **PodDisruptionBudget** - Ensures high availability during updates

### Stateless Design

This microservice:
- ❌ **Does NOT have a database** - All state managed externally
- ✅ Can scale horizontally without data sync
- ✅ Communicates with other services via REST APIs
- ✅ Session state managed by Firebase Authentication

### GCP Integration

Uses **Workload Identity** for secure GCP access:

```yaml
serviceAccount:
  annotations:
    iam.gke.io/gcp-service-account: "comments-likes-sa@graphite-plane-474510-s9.iam.gserviceaccount.com"
```

This provides:
- Firebase Authentication / Identity Platform integration
- Secure access to GCP services without keys

## Health Checks

The service exposes Quarkus health endpoints:

| Endpoint | Purpose | Used By |
|----------|---------|---------|
| `/q/health/started` | Application started | Startup probe |
| `/q/health/live` | Application alive | Liveness probe |
| `/q/health/ready` | Ready for traffic | Readiness probe |
| `/q/health` | Overall health | Manual checks |

### Test Health Endpoints

```bash
# Via port-forward
kubectl port-forward svc/comments-likes-service 8080:8080
curl http://localhost:8080/q/health

# Via ingress (production)
curl https://cl.tripico.fun/q/health
```

## Operations

### View Logs

```bash
# All pods
kubectl logs -l app=comments-likes-service --tail=100 -f

# Specific deployment
kubectl logs deployment/comments-likes-service -f

# Previous crashed container
kubectl logs <pod-name> --previous
```

### Check Status

```bash
# Deployment
kubectl get deployment comments-likes-service

# Pods
kubectl get pods -l app=comments-likes-service -o wide

# HPA status
kubectl get hpa comments-likes-service

# Service and endpoints
kubectl get svc,endpoints comments-likes-service

# Ingress
kubectl get ingress comments-likes-service

# TLS certificate
kubectl get certificate comments-likes-tls
```

### Scale Manually

```bash
# Scale to specific replica count
kubectl scale deployment comments-likes-service --replicas=5

# Or via Helm
helm upgrade comments-likes-service ./services/comments-likes-service/comments-likes-chart \
  --set replicaCount=5 \
  --reuse-values
```

### Debug Pod

```bash
# Describe pod for events
kubectl describe pod <pod-name>

# Get shell access
kubectl exec -it <pod-name> -- /bin/sh

# Check environment variables
kubectl exec <pod-name> -- env | sort
```

## Rollback

```bash
# List releases
helm history comments-likes-service

# Rollback to previous
helm rollback comments-likes-service

# Rollback to specific revision
helm rollback comments-likes-service 3

# Rollback with wait
helm rollback comments-likes-service --wait --timeout 5m
```

## Uninstall

```bash
helm uninstall comments-likes-service --namespace default
```

**Note:** This removes all resources except PersistentVolumeClaims (not applicable for this stateless service).

## Troubleshooting

### Pods Not Starting

**Check events:**
```bash
kubectl get events --sort-by='.lastTimestamp' | grep comments-likes
kubectl describe pod <pod-name>
```

**Common causes:**
- Image pull issues (check `imagePullPolicy`)
- Insufficient resources (check node capacity)
- Startup probe timing out

**Fix startup probe timeout:**
```yaml
# In values.yaml
probes:
  startup:
    failureThreshold: 60  # Increase from 30
```

### Certificate Issues

```bash
# Check certificate status
kubectl describe certificate comments-likes-tls

# Check cert-manager logs
kubectl logs -n cert-manager deployment/cert-manager -f

# Verify ClusterIssuer
kubectl get clusterissuer letsencrypt-prod
kubectl describe clusterissuer letsencrypt-prod
```

### Service Not Reachable

```bash
# Check if service has endpoints
kubectl get endpoints comments-likes-service

# Test internal connectivity
kubectl run curl-test --rm -it --image=curlimages/curl -- \
  curl -v http://comments-likes-service:8080/q/health

# Check ingress configuration
kubectl describe ingress comments-likes-service
```

### Authentication Problems

```bash
# Check Identity Platform configuration
kubectl get configmap comments-likes-service-config -o yaml | grep -i identity

# Check logs for auth errors
kubectl logs -l app=comments-likes-service | grep -i "auth\|token\|unauthorized"
```

### High Memory Usage

```bash
# Check current usage
kubectl top pods -l app=comments-likes-service

# Add JVM tuning in deployment (via values)
env:
  - name: JAVA_OPTS
    value: "-Xms256m -Xmx512m -XX:MaxMetaspaceSize=128m"
```

## CI/CD Integration

The GitHub Actions workflow automatically deploys when `Chart.yaml` changes:

**Trigger:** Changes to `services/comments-likes-service/comments-likes-chart/Chart.yaml`

**Workflow:**
1. Detects `appVersion` change in Chart.yaml
2. Builds Docker image with that version tag
3. Pushes to GHCR and GCP Artifact Registry
4. Deploys via Helm with production values
5. Verifies deployment health

**Manual Trigger:**
- Go to GitHub Actions tab
- Select the workflow
- Click "Run workflow"

## Development

### Local Testing (Minikube)

```bash
# Start minikube
minikube start

# Enable ingress
minikube addons enable ingress

# Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.3/cert-manager.yaml

# Deploy chart
helm install comments-likes ./services/comments-likes-service/comments-likes-chart \
  -f services/comments-likes-service/comments-likes-chart/values-dev.yaml

# Update /etc/hosts
echo "$(minikube ip) cl-dev.tripico.fun" | sudo tee -a /etc/hosts

# Test
curl http://cl-dev.tripico.fun/q/health
```

### Template Validation

```bash
# Render templates
helm template comments-likes ./services/comments-likes-service/comments-likes-chart \
  -f services/comments-likes-service/comments-likes-chart/values-prod.yaml

# Dry run
helm install comments-likes ./services/comments-likes-service/comments-likes-chart \
  --dry-run --debug \
  -f services/comments-likes-service/comments-likes-chart/values-prod.yaml

# Lint chart
helm lint ./services/comments-likes-service/comments-likes-chart
```

## Security

**Implemented:**
- ✅ TLS/SSL via cert-manager with Let's Encrypt
- ✅ GCP Workload Identity (no service account key files)
- ✅ Firebase Authentication for API protection
- ✅ Resource limits to prevent resource exhaustion
- ✅ Health checks for automatic failure recovery
- ✅ Stateless design (no sensitive data at rest)

**Recommendations:**
- 🔒 Enable Pod Security Standards (restricted mode)
- 🔒 Implement NetworkPolicies to restrict pod communication
- 🔒 Regular vulnerability scanning of container images
- 🔒 Enable audit logging
- 🔒 Run containers as non-root user

## Production Checklist

Before deploying to production:

- [ ] Update `appVersion` in Chart.yaml
- [ ] Test in development environment first
- [ ] Verify resource limits are appropriate
- [ ] Ensure HPA thresholds are tuned
- [ ] Confirm TLS certificate is valid
- [ ] Check Firebase Authentication configuration
- [ ] Review pod disruption budget settings
- [ ] Verify Workload Identity binding
- [ ] Test rollback procedure
- [ ] Document any custom values used

## Support

**For Issues:**
- Check [Troubleshooting](#troubleshooting) section above
- View logs: `kubectl logs -l app=comments-likes-service`
- Check events: `kubectl get events --sort-by='.lastTimestamp'`
- Verify connectivity: `kubectl get svc,endpoints comments-likes-service`

**Chart Information:**
- Chart Version: 1.0.0
- App Version: 2.1.6
- Maintainer: CAD Team (leotschritter@gmail.com)
- Domain: https://cl.tripico.fun

## License

Part of the CAD Travel Application project.

