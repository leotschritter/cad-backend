# Travel Warnings Helm Chart

A comprehensive Helm chart for deploying the Travel Warnings microservice with PostgreSQL database.

## Prerequisites

- Kubernetes 1.19+
- Helm 3.0+
- **NGINX Ingress Controller** (for ingress) - **REQUIRED**
- cert-manager (for TLS certificates in production)

### ⚠️ Important: Install NGINX Ingress Controller First!

Before installing this chart, you must install the NGINX Ingress Controller:

```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm install ingress-nginx ingress-nginx/ingress-nginx --namespace ingress-nginx --create-namespace
```

## Installation

### Local Development

```bash
# Install with default values (development mode)
helm install travel-warnings ./travel-warnings-chart

# With custom SMTP credentials
helm install travel-warnings ./travel-warnings-chart --set smtp.user=your-username --set smtp.password=your-password
```

### Production Deployment

```bash
# Create a custom values file for production
helm install travel-warnings ./travel-warnings-chart \
  -f values-prod.yaml \
  --set smtp.user=your-username \
  --set smtp.password=your-password
```

## Configuration

The following table lists the configurable parameters:

### Global Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `commonLabels` | Common labels applied to all resources | See values.yaml |

### PostgreSQL Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `postgres.name` | PostgreSQL service name | `warnings-postgres` |
| `postgres.image` | PostgreSQL image | `postgres:15-alpine` |
| `postgres.port` | PostgreSQL port | `5432` |
| `postgres.replicas` | Number of replicas | `1` |
| `postgres.environmentVariables.database` | Database name | `travel_warnings_db` |
| `postgres.environmentVariables.user` | Database user | `postgres` |
| `postgres.environmentVariables.password` | Database password | `postgres` |
| `postgresPvc.storage` | PVC storage size | `4Gi` |

### API Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `travelWarningsApi.name` | API service name | `travel-warnings-api` |
| `travelWarningsApi.image` | API image | `ghcr.io/leotschritter/cad-backend-travel-warnings:latest` |
| `travelWarningsApi.port` | API port | `8080` |
| `travelWarningsApi.replicas` | Number of replicas | `1` |
| `travelWarningsApi.resources.requests.memory` | Memory request | `512Mi` |
| `travelWarningsApi.resources.requests.cpu` | CPU request | `250m` |
| `travelWarningsApi.resources.limits.memory` | Memory limit | `1Gi` |
| `travelWarningsApi.resources.limits.cpu` | CPU limit | `1000m` |

### SMTP Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `smtp.user` | SMTP username | `""` (must be set) |
| `smtp.password` | SMTP password | `""` (must be set) |
| `travelWarningsConfig.smtp.from` | Email from address | `noreply.travel-warning@htwg-konstanz.de` |
| `travelWarningsConfig.smtp.host` | SMTP host | `smtp.htwg-konstanz.de` |
| `travelWarningsConfig.smtp.port` | SMTP port | `587` |
| `travelWarningsConfig.smtp.startTls` | SMTP TLS setting | `REQUIRED` |

### Ingress Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `ingress.name` | Ingress name | `travel-warnings-ingress` |
| `ingress.dev.host` | Development hostname | `travel-warnings.local` |
| `ingress.prod.host` | Production hostname | `warnings.tripico.fun` |
| `ingress.prod.tlsSecretName` | TLS secret name | `warnings-tls` |

## Upgrading

```bash
# Upgrade with new values
helm upgrade travel-warnings ./travel-warnings-chart \
  --set travelWarningsApi.replicas=2

# Upgrade with new image version
helm upgrade travel-warnings ./travel-warnings-chart \
  --set travelWarningsApi.image=ghcr.io/leotschritter/cad-backend-travel-warnings:v2.0.0
```

## Uninstallation

```bash
helm uninstall travel-warnings

# Also delete PVC if needed
kubectl delete pvc warnings-postgres-data-pvc
```

## Troubleshooting

### Database Connection Issues

```bash
# Check postgres pod status
kubectl get pods -l app=warnings-postgres

# Check postgres logs
kubectl logs -l app=warnings-postgres

# Connect to postgres
kubectl exec -it $(kubectl get pod -l app=warnings-postgres -o name) -- psql -U postgres -d travel_warnings_db
```

### API Health Issues

```bash
# Check API health endpoints
kubectl port-forward svc/travel-warnings-api 8080:8080
curl http://localhost:8080/q/health/live
curl http://localhost:8080/q/health/ready

# View detailed health info
curl http://localhost:8080/q/health
```

### SMTP Issues

Ensure SMTP credentials are set:
```bash
helm upgrade travel-warnings ./travel-warnings-chart \
  --set smtp.user=your-username \
  --set smtp.password=your-password
```

### Ingress Issues

```bash
# Check ingress status
kubectl get ingress travel-warnings-ingress

# Check ingress controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx
```

## Files Excluded from Local Deployment

The following files are excluded via `.helmignore` for local development:
- `templates/ingress-prod.yaml` - Production ingress with TLS
- `templates/certificate.yaml` - cert-manager Certificate
- `templates/deploy-local.sh` - Local deployment script

## Architecture

```
┌─────────────────┐
│     Ingress     │
│   (NGINX)       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   API Service   │
│   (ClusterIP)   │
└────────┬────────┘
         │
         ▼
┌─────────────────────────┐
│  API Deployment         │
│  - Health Checks        │
│  - Resource Limits      │
│  - Rolling Updates      │
└────────┬────────────────┘
         │
         ▼
┌─────────────────┐      ┌──────────────┐
│  PostgreSQL     │◄─────┤    PVC       │
│  Service        │      │   (4Gi)      │
└─────────────────┘      └──────────────┘
```

## License

This chart is part of the Travel Warnings microservice project.

