# Cert-Manager Setup Helm Chart

This Helm chart installs cert-manager and configures a ClusterIssuer for Let's Encrypt SSL/TLS certificates.

## Features

- Installs cert-manager v1.13.3 with CRDs
- Creates a production ClusterIssuer for Let's Encrypt
- Optional staging ClusterIssuer for testing
- Configures HTTP-01 ACME challenge solver with NGINX ingress

## Prerequisites

- Kubernetes cluster (GKE, EKS, AKS, or any Kubernetes 1.21+)
- kubectl configured to communicate with your cluster
- Helm 3.x installed
- NGINX Ingress Controller installed in the cluster

## Installation

### Quick Install

```bash
# Add Jetstack Helm repository (for cert-manager dependency)
helm repo add jetstack https://charts.jetstack.io
helm repo update

# Install the chart
helm install cert-manager-setup ./kubernetes/cert-manager-chart \
  --namespace cert-manager \
  --create-namespace \
  --wait --timeout 5m
```

### Install with Custom Email

```bash
helm install cert-manager-setup ./kubernetes/cert-manager-chart \
  --namespace cert-manager \
  --create-namespace \
  --set clusterIssuer.email=your-email@example.com \
  --wait --timeout 5m
```

### Install with Staging Issuer (for testing)

```bash
helm install cert-manager-setup ./kubernetes/cert-manager-chart \
  --namespace cert-manager \
  --create-namespace \
  --set stagingIssuer.enabled=true \
  --wait --timeout 5m
```

## Verification

After installation, verify that cert-manager is running:

```bash
# Check cert-manager pods
kubectl get pods -n cert-manager

# Check ClusterIssuers
kubectl get clusterissuer

# Describe the ClusterIssuer to see its status
kubectl describe clusterissuer letsencrypt-prod
```

Expected output for ClusterIssuer:
```
Status:
  Acme:
    Last Registered Email:  benedikt.scheffel@gmail.com
    Uri:                    https://acme-v02.api.letsencrypt.org/acme/acct/...
  Conditions:
    Last Transition Time:  2025-11-26T...
    Message:               The ACME account was registered with the ACME server
    Reason:                ACMEAccountRegistered
    Status:                True
    Type:                  Ready
```

## Usage with Certificates

Once installed, you can reference the ClusterIssuer in your Certificate resources:

```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: example-tls-cert
  namespace: default
spec:
  secretName: example-tls-secret
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
    - example.com
    - www.example.com
```

Or in your Ingress resources:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: example-ingress
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - example.com
      secretName: example-tls-secret
  rules:
    - host: example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: example-service
                port:
                  number: 80
```

## Configuration

### Values

| Parameter | Description | Default |
|-----------|-------------|---------|
| `cert-manager.installCRDs` | Install cert-manager CRDs | `true` |
| `clusterIssuer.enabled` | Create production ClusterIssuer | `true` |
| `clusterIssuer.name` | Name of the ClusterIssuer | `letsencrypt-prod` |
| `clusterIssuer.email` | Email for Let's Encrypt registration | `benedikt.scheffel@gmail.com` |
| `clusterIssuer.server` | ACME server URL | `https://acme-v02.api.letsencrypt.org/directory` |
| `stagingIssuer.enabled` | Create staging ClusterIssuer | `false` |
| `stagingIssuer.name` | Name of staging ClusterIssuer | `letsencrypt-staging` |

### Custom Values File

Create a `custom-values.yaml`:

```yaml
clusterIssuer:
  email: your-email@example.com

stagingIssuer:
  enabled: true

cert-manager:
  resources:
    requests:
      cpu: 20m
      memory: 64Mi
```

Install with custom values:

```bash
helm install cert-manager-setup ./kubernetes/cert-manager-chart \
  -f custom-values.yaml \
  --namespace cert-manager \
  --create-namespace
```

## Upgrade

```bash
helm upgrade cert-manager-setup ./kubernetes/cert-manager-chart \
  --namespace cert-manager \
  --wait --timeout 5m
```

## Uninstall

```bash
helm uninstall cert-manager-setup --namespace cert-manager
```

**Note**: This will remove cert-manager and all ClusterIssuers. Existing certificates will continue to work until they expire.

## Troubleshooting

### Check cert-manager logs

```bash
kubectl logs -n cert-manager deployment/cert-manager-setup-cert-manager -f
```

### Check ClusterIssuer status

```bash
kubectl describe clusterissuer letsencrypt-prod
```

### Check Certificate status

```bash
kubectl describe certificate <certificate-name> -n <namespace>
```

### Common Issues

1. **ClusterIssuer not found**: Wait a few seconds after installation for cert-manager to start
2. **ACME challenge failing**: Ensure NGINX ingress controller is installed and working
3. **Rate limiting**: Use staging issuer for testing to avoid Let's Encrypt rate limits

## Integration with CI/CD

Add this step to your GitHub Actions workflow:

```yaml
- name: Install cert-manager and ClusterIssuer
  run: |
    helm repo add jetstack https://charts.jetstack.io
    helm repo update
    helm dependency update ./kubernetes/cert-manager-chart
    helm upgrade --install cert-manager-setup ./kubernetes/cert-manager-chart \
      --namespace cert-manager \
      --create-namespace \
      --wait --timeout 5m
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=cert-manager \
      -n cert-manager --timeout=2m
    kubectl get clusterissuer letsencrypt-prod
```

## License

This chart is provided as-is for the CAD project.
