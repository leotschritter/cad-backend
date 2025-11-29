# Kubernetes Deployment Guide

## Prerequisites

- Kubernetes cluster (GKE, EKS, or local)
- kubectl configured
- Docker image built and pushed to registry

## Building and Pushing the Docker Image

```bash
# Build the application
cd recommendation-service
./mvnw clean package

# Build Docker image
docker build -t gcr.io/YOUR_PROJECT_ID/recommendation-service:latest .

# Push to Google Container Registry
docker push gcr.io/YOUR_PROJECT_ID/recommendation-service:latest
```

## Creating Secrets

### Database Secret
```bash
kubectl create secret generic recommendation-db-secret \
  --from-literal=username=postgres \
  --from-literal=password=YOUR_PASSWORD
```

### Firebase Credentials
```bash
kubectl create secret generic firebase-credentials \
  --from-file=credentials.json=/path/to/firebase-credentials.json
```

## Updating ConfigMap

Edit `k8s/configmap.yaml` and replace:
- `CLOUD_SQL_INSTANCE_CONNECTION_NAME` with your Cloud SQL instance connection name
- Verify `itinerary-service-url` points to correct service

## Deploying to Kubernetes

```bash
cd recommendation-service/k8s

# Apply all manifests
kubectl apply -f configmap.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
kubectl apply -f hpa.yaml
kubectl apply -f ingress.yaml
```

## Verifying Deployment

```bash
# Check pods
kubectl get pods -l app=recommendation-service

# Check service
kubectl get svc recommendation-service

# Check logs
kubectl logs -l app=recommendation-service

# Check HPA status
kubectl get hpa recommendation-service-hpa
```

## Testing the Service

```bash
# Port forward for local testing
kubectl port-forward svc/recommendation-service 8083:8083

# Test the API
curl http://localhost:8083/api/v1/feed?travellerId=test-user

# Check health
curl http://localhost:8083/q/health
```

## Ingress Configuration

Update `k8s/ingress.yaml`:
- Replace `api.yourdomain.com` with your actual domain
- Ensure cert-manager is installed for TLS certificates

## Scaling

### Manual Scaling
```bash
kubectl scale deployment recommendation-service --replicas=5
```

### Autoscaling
The HPA will automatically scale between 2-10 replicas based on CPU and memory usage.

## Monitoring

```bash
# Watch pod metrics
kubectl top pods -l app=recommendation-service

# View HPA metrics
kubectl describe hpa recommendation-service-hpa
```

## Troubleshooting

### Pod not starting
```bash
kubectl describe pod <pod-name>
kubectl logs <pod-name>
```

### Database connection issues
```bash
# Verify secrets
kubectl get secret recommendation-db-secret -o yaml

# Test connection from pod
kubectl exec -it <pod-name> -- /bin/bash
```

### Health check failures
```bash
# Check readiness
kubectl get pods -l app=recommendation-service

# View health endpoint
kubectl port-forward svc/recommendation-service 8083:8083
curl http://localhost:8083/q/health
```

## Cleanup

```bash
kubectl delete -f k8s/
kubectl delete secret recommendation-db-secret
kubectl delete secret firebase-credentials
```

