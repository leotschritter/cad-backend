# Tenant Service Helm Chart

This Helm chart deploys the Tenant Service for the Travel Planning Application, including a MongoDB database with persistent storage.

## Prerequisites

- Kubernetes 1.19+
- Helm 3.0+
- Nginx Ingress Controller
- cert-manager (for TLS certificates)
- Persistent Volume support for MongoDB

## Installation

### Development Environment

```bash
helm upgrade --install tenant-service ./tenant-service-chart \
  -f ./tenant-service-chart/values-dev.yaml \
  --namespace default
```

### Production Environment

**Important**: Override the MongoDB password in production!

```bash
helm upgrade --install tenant-service ./tenant-service-chart \
  -f ./tenant-service-chart/values-prod.yaml \
  --set mongodb.auth.rootPassword="your-secure-password" \
  --namespace default
```

## Configuration

See `values.yaml` for the default configuration options.
See `values-dev.yaml` and `values-prod.yaml` for environment-specific overrides.

### MongoDB Configuration

The chart includes a MongoDB StatefulSet with persistent storage:

- **Development**: MongoDB with no authentication, 5Gi storage
- **Production**: MongoDB with authentication enabled, 20Gi storage

Key MongoDB settings in `values.yaml`:
- `mongodb.enabled`: Enable/disable MongoDB deployment (default: true)
- `mongodb.persistence.size`: PVC size for data (default: 10Gi)
- `mongodb.auth.enabled`: Enable authentication (default: false, set to true in prod)
- `mongodb.auth.rootUsername/rootPassword`: Admin credentials
- `mongodb.persistence.storageClass`: Storage class for PVC (default: cluster default)

### MongoDB Storage Classes

You may need to specify a storage class based on your cluster:

```bash
# For GKE standard persistent disk
--set mongodb.persistence.storageClass=standard

# For GKE SSD persistent disk
--set mongodb.persistence.storageClass=standard-rwo
```

### Disable MongoDB

If you want to use an external MongoDB:

```bash
helm upgrade --install tenant-service ./tenant-service-chart \
  -f ./tenant-service-chart/values-dev.yaml \
  --set mongodb.enabled=false \
  --set mongodb.connectionString="mongodb://external-host:27017" \
  --namespace default
```

## Accessing MongoDB

### In Development (no auth)

Port-forward to access MongoDB:

```bash
kubectl port-forward svc/mongodb-service 27017:27017
mongosh mongodb://localhost:27017/tenant_db
```

### In Production (with auth)

```bash
kubectl port-forward svc/mongodb-service 27017:27017
mongosh mongodb://admin:your-password@localhost:27017/tenant_db?authSource=admin
```

## Backup and Restore

### Backup

```bash
kubectl exec -it mongodb-0 -- mongodump --out /tmp/backup
kubectl cp mongodb-0:/tmp/backup ./backup
```

### Restore

```bash
kubectl cp ./backup mongodb-0:/tmp/backup
kubectl exec -it mongodb-0 -- mongorestore /tmp/backup
```

## Uninstallation

```bash
helm uninstall tenant-service --namespace default
```

**Note**: This will NOT delete the PersistentVolumeClaims (PVCs) automatically. To delete them:

```bash
kubectl delete pvc -l app=mongodb
```

