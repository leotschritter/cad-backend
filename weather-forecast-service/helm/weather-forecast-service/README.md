# Weather Forecast Service Helm Chart

This Helm chart deploys the Weather Forecast Service along with a PostgreSQL database to Kubernetes, following the same pattern as the Travel Warnings service.

## Architektur

### Deployment Flow

```
GitHub Push → GitHub Actions → GKE Cluster
              (Build & Deploy)
```

**Pipeline Schritte:**
1. Build Docker Image
2. Push zu GHCR & GCP Artifact Registry
3. Deploy zu GKE mit Helm

### Kubernetes Komponenten

```
Internet (HTTPS)
    ↓
Ingress (weather.tripico.fun + TLS)
    ↓
Weather Forecast Service (Port 8080)
    ├─ Init: wait-for-postgres
    └─ App: Quarkus (2 Replicas)
         ↓
PostgreSQL (Port 5432 + 5Gi Storage)
```

### Erstellte Resources

| Resource | Beschreibung |
| --- | --- |
| 2x Deployments | API (Quarkus) + DB (PostgreSQL) |
| 2x Services | ClusterIP für API & DB |
| 1x Ingress | NGINX mit TLS |
| 1x ConfigMap | App-Konfiguration |
| 1x Secret | Meteosource API Key |
| 1x PVC | 5Gi PostgreSQL Storage |
| 1x Certificate | TLS für Domain |

## Prerequisites

- Kubernetes 1.19+
- Helm 3.0+
- cert-manager (for TLS certificates in production)
- nginx-ingress-controller

## Deployment Methods

Es gibt zwei Hauptmethoden für das Deployment:

1. **Automatisches Deployment via GitHub Actions** (empfohlen für Production)
2. **Lokales/Manuelles Deployment** (für Entwicklung oder manuelle Deployments)

---

## 1. Automatisches Deployment via GitHub Actions

### 1.1 Workflow-Trigger

Das Deployment wird automatisch ausgelöst bei:
- **Push auf `main` oder `feature/wf-helm` Branch**
- **Erstellung von Tags** im Format `v*.*.*`
- **Änderungen** im `weather-forecast-service/` Verzeichnis
- **Manuell** via `workflow_dispatch`

### 1.2 Workflow-Ablauf

Der GitHub Actions Workflow (`.github/workflows/ghcr-and-gcp-weather-forecast.yml`) führt folgende Schritte aus:

#### Build & Push Job
1. Docker Image bauen
2. Push zu **GitHub Container Registry (GHCR)**: `ghcr.io/leotschritter/weather-forecast-service`
3. Push zu **GCP Artifact Registry**: `europe-west1-docker.pkg.dev/graphite-plane-474510-s9/docker-repo/weather-forecast-service`
4. Tagging mit `latest` und Git SHA

#### Deploy Job
1. GKE-Cluster Verbindung: `tripico-cluster` in `europe-west1`
2. NGINX Ingress Controller Installation (falls nicht vorhanden)
3. Helm Deployment:
   - Verwendet `values-prod.yaml`
   - Setzt Meteosource API Key aus GitHub Secrets
   - Verwendet frisch gebautes Image mit Git SHA
   - `helm upgrade --install` für idempotentes Deployment
4. Deployment-Verifizierung

### 1.3 Benötigte GitHub Secrets

Folgende Secrets müssen im Repository konfiguriert sein:

| Secret | Beschreibung |
|--------|--------------|
| `GCP_SERVICE_ACCOUNT_KEY` | GCP Service Account JSON für GKE-Zugriff |
| `METEOSOURCE_API_KEY` | API Key für Meteosource Weather API |

### 1.4 Manuelle Workflow-Ausführung

```bash
# Via GitHub UI: Actions → ghcr-and-gcp-weather-forecast.yml → Run workflow

# Oder via GitHub CLI:
gh workflow run ghcr-and-gcp-weather-forecast.yml
```

### 1.5 Deployment überwachen

Nach dem Push können Sie den Workflow-Status verfolgen:
- GitHub → Actions Tab
- Logs für `build-and-push` und `deploy` Jobs

---

## 2. Lokales/Manuelles Deployment

### 2.1 Voraussetzungen für lokales Deployment

1. **kubectl** konfiguriert für Ziel-Cluster
2. **Helm 3+** installiert
3. **Meteosource API Key** verfügbar

### 2.2 Development/Local Deployment

```bash
# Installation
helm install weather-forecast-service ./helm/weather-forecast-service \
  -f ./helm/weather-forecast-service/values-dev.yaml \
  --set meteosource.apiKey=YOUR_API_KEY

# Zugriff via Port-Forward
kubectl port-forward svc/weather-forecast-service 8080:8080
```

**Dev:** 1 Replica, 256Mi RAM, kein TLS

### 2.3 Production Deployment auf GKE

```bash
# GKE Verbinden
gcloud container clusters get-credentials tripico-cluster --region europe-west1

# Installation
helm install weather-forecast-service ./helm/weather-forecast-service \
  -f ./helm/weather-forecast-service/values-prod.yaml \
  --set meteosource.apiKey=YOUR_API_KEY \
  --wait --timeout 5m
```

**Prod:** 2 Replicas, 512Mi RAM, TLS, Domain: weather.tripico.fun

## Configuration

### Wichtige Parameter

| Parameter | Dev | Prod |
| --- | --- | --- |
| Replicas | 1 | 2 |
| Resources | 256Mi/125m | 512Mi/250m |
| TLS | Nein | Ja |
| Host | weather-forecast.local | weather.tripico.fun |
| meteosource.apiKey | **Pflicht** | **Pflicht** |

## Upgrade & Verwaltung

```bash
# Automatisch via GitHub Actions bei Push auf main/feature/wf-helm

# Manuell upgraden
helm upgrade weather-forecast-service ./helm/weather-forecast-service \
  -f ./helm/weather-forecast-service/values-prod.yaml \
  --set meteosource.apiKey=YOUR_API_KEY

# Deinstallieren (⚠️ löscht auch PVC/Daten)
helm uninstall weather-forecast-service

# Status prüfen
kubectl get pods,svc,ingress
kubectl logs -l app=weather-forecast-service -f
```

## Technische Details

### Init Container
Ein Init Container (`wait-for-postgres`) verhindert CrashLoopBackOff, indem er wartet bis PostgreSQL bereit ist:
```bash
until nc -z weather-postgres 5432; do sleep 2; done
```

### Health Probes
- **PostgreSQL**: Readiness/Liveness via `pg_isready`
- **API**: Startup/Readiness/Liveness via Quarkus `/q/health/*` Endpoints

## Troubleshooting

### Häufige Probleme

**CrashLoopBackOff / DB Connection Failed**
```bash
# Init Container & DB Logs prüfen
kubectl logs -l app=weather-forecast-service -c wait-for-postgres
kubectl logs -l app=weather-postgres
```

**Pod startet nicht**
```bash
kubectl describe pod <pod-name>
kubectl logs <pod-name>
```

**API Key vergessen**
```bash
helm upgrade weather-forecast-service ./helm/weather-forecast-service \
  -f ./helm/weather-forecast-service/values-prod.yaml \
  --set meteosource.apiKey=YOUR_API_KEY
```

**GitHub Actions Failed**
- GitHub Actions Logs prüfen
- Secrets validieren: `GCP_SERVICE_ACCOUNT_KEY`, `METEOSOURCE_API_KEY`
