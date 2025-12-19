# Multi-Namespace Kubernetes Architektur

## Übersicht

Diese Architektur implementiert eine Multi-Tenant Lösung mit drei Namespaces:

- **`shared`**: Gemeinsam genutzte Services (weather-forecast, travel-warnings)
- **`freemium`**: Services für Freemium-Kunden mit niedrigeren Resource Quotas
- **`standard`**: Services für Standard-Kunden mit höheren Resource Quotas

## Architektur

```
┌─────────────────────────────────────────────────────────┐
│                 GKE Cluster (Shared)                    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────────┐  ┌──────────────────┐           │
│  │  Namespace:      │  │  Namespace:      │           │
│  │  shared          │  │  freemium        │           │
│  ├──────────────────┤  ├──────────────────┤           │
│  │ • weather        │  │ • itinerary      │           │
│  │ • travel-warnings│  │ • recommendation │           │
│  │                  │  │ • user, comment  │           │
│  │ (1x deployed)    │  │   like, location │           │
│  └──────────────────┘  └──────────────────┘           │
│         ▲                      │                       │
│         │                      │                       │
│         └──────────────────────┘                       │
│            Network Policy                              │
│            (freemium → shared allowed)                 │
│                                                         │
│  ┌──────────────────┐                                  │
│  │  Namespace:      │                                  │
│  │  standard             │                                  │
│  ├──────────────────┤                                  │
│  │ • itinerary      │                                  │
│  │ • recommendation │                                  │
│  │ • user, comment  │                                  │
│  │   like, location │                                  │
│  └──────────────────┘                                  │
│         ▲                                               │
│         │                                               │
│         └───────────────────────────────────────────┐  │
│            Network Policy                           │  │
│            (standard → shared allowed)                   │  │
│                                                         │
└─────────────────────────────────────────────────────────┘

Network Isolation: freemium ⇄ standard (blocked)
```

## Resource Quotas

### Shared Namespace
- CPU: 20 vCPU (requests), 40 vCPU (limits)
- Memory: 40Gi (requests), 80Gi (limits)
- Pods: 100
- LoadBalancers: 5

### Freemium Namespace
- CPU: 10 vCPU (requests), 20 vCPU (limits)
- Memory: 20Gi (requests), 40Gi (limits)
- Pods: 50
- LoadBalancers: 3

### Standard Namespace
- CPU: 50 vCPU (requests), 100 vCPU (limits)
- Memory: 100Gi (requests), 200Gi (limits)
- Pods: 200
- LoadBalancers: 10

## Network Policies

- **Isolation zwischen freemium ↔ standard**: Kein direkter Traffic möglich
- **Zugriff auf shared**: Beide Namespaces können auf shared Services zugreifen
- **DNS**: Alle Namespaces können DNS-Queries durchführen
- **External Traffic**: HTTPS/HTTP nach außen erlaubt

## Deployment

### Option 1: GitHub Actions (empfohlen)

1. Gehe zu **Actions** → **Deploy Services to Multi-Namespace Architecture**
2. Klicke auf **Run workflow**
3. Wähle aus:
   - Deploy shared services: ✓
   - Deploy freemium tier: ✓
   - Deploy standard tier: ✓
   - Environment: **prod** oder **dev**

**Unterschiede zwischen Dev und Prod:**
- **Dev**: Separate Dev-Datenbanken, Dev-Domains (`*-dev.tripico.fun`)
- **Prod**: Production-Datenbanken, Prod-Domains (`*.tripico.fun`)
- **Resources und Auth**: Identisch in beiden Environments
- **Domains werden automatisch gesetzt**: Die Pipeline setzt die Domains basierend auf Environment (dev/prod), Tier (freemium/standard) und Service-Name

### Option 2: Manuelles Deployment

#### 1. Namespaces und Policies erstellen

```bash
# Namespaces erstellen
kubectl apply -f infrastructure/kubernetes/namespaces/shared-namespace.yaml
kubectl apply -f infrastructure/kubernetes/namespaces/freemium-namespace.yaml
kubectl apply -f infrastructure/kubernetes/namespaces/standard-namespace.yaml

# Network Policies anwenden
kubectl apply -f infrastructure/kubernetes/namespaces/network-policies.yaml

# Verifizieren
kubectl get namespaces shared freemium standard
kubectl get networkpolicies --all-namespaces
```

#### 2. Shared Services deployen

**Für Production:**
```bash
# Weather Service
helm upgrade --install weather-forecast-service \
  services/weather-forecast-service/helm/weather-forecast-service \
  --namespace shared \
  --values services/weather-forecast-service/helm/weather-forecast-service/values-shared-prod.yaml \
  --set meteosource.apiKey=YOUR_API_KEY

# Travel Warnings Service
helm upgrade --install travel-warnings-service \
  services/travel_warnings/travel-warnings-chart \
  --namespace shared \
  --values services/travel_warnings/travel-warnings-chart/values-shared-prod.yaml
```

**Für Development:**
```bash
# Weather Service
helm upgrade --install weather-forecast-service \
  services/weather-forecast-service/helm/weather-forecast-service \
  --namespace shared \
  --values services/weather-forecast-service/helm/weather-forecast-service/values-shared-dev.yaml \
  --set meteosource.apiKey=YOUR_API_KEY

# Travel Warnings Service
helm upgrade --install travel-warnings-service \
  services/travel_warnings/travel-warnings-chart \
  --namespace shared \
  --values services/travel_warnings/travel-warnings-chart/values-shared-dev.yaml
```

#### 3. Freemium Services deployen

**Für Production:**
```bash
# Itinerary Service
helm upgrade --install itinerary-service-freemium \
  services/itinerary-service/kubernetes/itinerary-service-chart \
  --namespace freemium \
  --values services/itinerary-service/kubernetes/itinerary-service-chart/values-freemium-prod.yaml \
  --set database.password=YOUR_PASSWORD \
  --set database.url=YOUR_DB_URL

# Recommendation Service
helm upgrade --install recommendation-service-freemium \
  services/recommendation-service/recommendation-service-chart \
  --namespace freemium \
  --values services/recommendation-service/recommendation-service-chart/values-freemium-prod.yaml \
  --set neo4j.password=YOUR_NEO4J_PASSWORD
```

**Für Development:**
```bash
# Itinerary Service
helm upgrade --install itinerary-service-freemium \
  services/itinerary-service/kubernetes/itinerary-service-chart \
  --namespace freemium \
  --values services/itinerary-service/kubernetes/itinerary-service-chart/values-freemium-dev.yaml \
  --set database.password=YOUR_DEV_PASSWORD \
  --set database.url=YOUR_DEV_DB_URL

# Recommendation Service
helm upgrade --install recommendation-service-freemium \
  services/recommendation-service/recommendation-service-chart \
  --namespace freemium \
  --values services/recommendation-service/recommendation-service-chart/values-freemium-dev.yaml \
  --set neo4j.password=YOUR_DEV_NEO4J_PASSWORD
```

#### 4. Standard Services deployen

**Für Production:**
```bash
# Itinerary Service
helm upgrade --install itinerary-service-standard \
  services/itinerary-service/kubernetes/itinerary-service-chart \
  --namespace standard \
  --values services/itinerary-service/kubernetes/itinerary-service-chart/values-standard-prod.yaml \
  --set database.password=YOUR_PASSWORD \
  --set database.url=YOUR_DB_URL

# Recommendation Service
helm upgrade --install recommendation-service-standard \
  services/recommendation-service/recommendation-service-chart \
  --namespace standard \
  --values services/recommendation-service/recommendation-service-chart/values-standard-prod.yaml \
  --set neo4j.password=YOUR_NEO4J_PASSWORD
```

**Für Development:**
```bash
# Itinerary Service
helm upgrade --install itinerary-service-standard \
  services/itinerary-service/kubernetes/itinerary-service-chart \
  --namespace standard \
  --values services/itinerary-service/kubernetes/itinerary-service-chart/values-standard-dev.yaml \
  --set database.password=YOUR_DEV_PASSWORD \
  --set database.url=YOUR_DEV_DB_URL

# Recommendation Service
helm upgrade --install recommendation-service-standard \
  services/recommendation-service/recommendation-service-chart \
  --namespace standard \
  --values services/recommendation-service/recommendation-service-chart/values-standard-dev.yaml \
  --set neo4j.password=YOUR_DEV_NEO4J_PASSWORD
```

### Option 3: Deployment-Skript

```bash
# Alle Namespaces deployen
./scripts/deploy-multi-namespace.sh

# Nur shared deployen
./scripts/deploy-multi-namespace.sh --no-freemium --no-standard

# Nur freemium deployen
./scripts/deploy-multi-namespace.sh --no-shared --no-standard
```

## Secrets Management

Folgende Secrets müssen in GitHub Actions / lokaler Umgebung gesetzt werden:

### GitHub Secrets (Repository Secrets)

**Production Secrets:**
```
GCP_SERVICE_ACCOUNT_KEY
METEOSOURCE_API_KEY
DB_PASSWORD_FREEMIUM
DB_URL_FREEMIUM
NEO4J_PASSWORD_FREEMIUM
DB_PASSWORD_STANDARD
DB_URL_STANDARD
NEO4J_PASSWORD_STANDARD
```

**Development Secrets:**
```
DB_PASSWORD_FREEMIUM_DEV
DB_URL_FREEMIUM_DEV
NEO4J_PASSWORD_FREEMIUM_DEV
DB_PASSWORD_STANDARD_DEV
DB_URL_STANDARD_DEV
NEO4J_PASSWORD_STANDARD_DEV
```

**Repository Variables:**
```
PROJECT_ID
```

### Lokale Umgebungsvariablen
```bash
export GCP_PROJECT_ID="graphite-plane-474510-s9"
export METEOSOURCE_API_KEY="your-key"
export DB_PASSWORD_FREEMIUM="your-password"
export DB_URL_FREEMIUM="jdbc:postgresql://..."
export NEO4J_PASSWORD_FREEMIUM="your-password"
export DB_PASSWORD_STANDARD="your-password"
export DB_URL_STANDARD="jdbc:postgresql://..."
export NEO4J_PASSWORD_STANDARD="your-password"
```

## Service Discovery

### Intra-Namespace Communication
Services im gleichen Namespace:
```
http://service-name:port
```

### Cross-Namespace Communication (nur zu shared)
Von freemium/standard zu shared:
```
http://weather-forecast-service.shared.svc.cluster.local:8080
http://travel-warnings-service.shared.svc.cluster.local:8080
```

### External Access

Die Domains werden automatisch via Pipeline gesetzt basierend auf:
- Environment (dev/prod)
- Tier (freemium/standard/shared)
- Service-Name

**Domain-Schema:**

**Production:**
- Freemium: `https://{service-name}-freemium.tripico.fun` (z.B. `itinerary-freemium.tripico.fun`)
- Standard: `https://{service-name}-standard.tripico.fun` (z.B. `itinerary-standard.tripico.fun`)
- Shared: `https://{service}.tripico.fun` (z.B. `weather.tripico.fun`, `warnings.tripico.fun`)

**Development:**
- Freemium: `https://{service-name}-freemium-dev.tripico.fun`
- Standard: `https://{service-name}-standard-dev.tripico.fun`
- Shared: `https://{service}-dev.tripico.fun`

**Beispiele:**
```
# Production
https://itinerary-freemium.tripico.fun
https://recommendation-standard.tripico.fun
https://weather.tripico.fun

# Development
https://itinerary-freemium-dev.tripico.fun
https://recommendation-standard-dev.tripico.fun
https://weather-dev.tripico.fun
```

## Monitoring & Debugging

### Resource Usage überprüfen
```bash
# Quota-Nutzung anzeigen
kubectl describe quota -n shared
kubectl describe quota -n freemium
kubectl describe quota -n standard

# Pods pro Namespace
kubectl get pods -n shared
kubectl get pods -n freemium
kubectl get pods -n standard

# Services
kubectl get svc --all-namespaces
```

### Network Policies testen
```bash
# Von freemium zu shared (sollte funktionieren)
kubectl exec -n freemium <pod-name> -- curl http://weather-forecast-service.shared.svc.cluster.local:8080/q/health

# Von freemium zu standard (sollte fehlschlagen)
kubectl exec -n freemium <pod-name> -- curl http://itinerary-service.standard.svc.cluster.local:8080/q/health
```

### Logs
```bash
kubectl logs -n shared deployment/weather-forecast-service
kubectl logs -n freemium deployment/itinerary-service
kubectl logs -n standard deployment/recommendation-service
```

## Troubleshooting

### Problem: Pod startet nicht wegen Resource Quota
```bash
# Quota-Limits erhöhen in den entsprechenden YAML-Dateien
# Dann neu anwenden:
kubectl apply -f infrastructure/kubernetes/namespaces/<namespace>-namespace.yaml
```

### Problem: Network Policy blockiert gewünschten Traffic
```bash
# Network Policies anzeigen
kubectl describe networkpolicy -n freemium

# Temporär deaktivieren (NUR für Debugging!)
kubectl delete networkpolicy -n freemium freemium-isolation
```

### Problem: Service nicht erreichbar
```bash
# DNS-Auflösung testen
kubectl run -n freemium debug --image=busybox --rm -it --restart=Never -- nslookup weather-forecast-service.shared.svc.cluster.local

# Service Endpoints überprüfen
kubectl get endpoints -n shared weather-forecast-service
```

## Nächste Schritte

1. **DNS-Konfiguration**: Domains auf die Ingress IPs zeigen lassen
2. **Monitoring**: Prometheus/Grafana für Namespace-spezifisches Monitoring
3. **Alerting**: Alerts für Quota-Überschreitungen
4. **Cost Tracking**: GCP Labels für Cost-Attribution pro Namespace
5. **Enterprise Cluster**: Separates Cluster für Enterprise-Kunden aufsetzen
