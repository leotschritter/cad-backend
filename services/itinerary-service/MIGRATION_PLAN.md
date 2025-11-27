# Microservices Migration Plan

## Phase 1: Repository Restructuring (1-2 Tage)

### 1.1 Neues Branch erstellen
```bash
git checkout -b feature/microservices-restructure
```

### 1.2 Ordnerstruktur erstellen
```bash
# Services Ordner erstellen
mkdir -p services/{itinerary-service,weather-forecast-service,travel-warnings-service}

# Itinerary Service (aktuelles Hauptprojekt)
mv src services/itinerary-service/
mv pom.xml services/itinerary-service/
mv Dockerfile services/itinerary-service/

# Weather Forecast Service
mv weather-forecast-service/* services/weather-forecast-service/
mv Weather.Dockerfile services/weather-forecast-service/Dockerfile

# Travel Warnings Service
mv travel_warnings/* services/travel-warnings-service/
mv TravelWarnings.Dockerfile services/travel-warnings-service/Dockerfile

# Cleanup
rmdir weather-forecast-service travel_warnings
```

### 1.3 Kubernetes Charts umbenennen
```bash
# Im kubernetes/ Ordner umbenennen falls nötig
mv kubernetes/itinerary-service kubernetes/itinerary-service-chart
mv weather-forecast-service/helm kubernetes/weather-forecast-service-chart
mv travel_warnings/travel-warnings-chart kubernetes/travel-warnings-service-chart
```

## Phase 2: Pipeline-Anpassung (2-3 Tage)

### 2.1 Workflow-Dateien erstellen

**Datei: `.github/workflows/deploy-itinerary.yml`**
- Trigger: `paths: ['services/itinerary-service/**', 'kubernetes/itinerary-service-chart/**']`
- Build Context: `services/itinerary-service`
- Dockerfile: `services/itinerary-service/Dockerfile`

**Datei: `.github/workflows/deploy-weather.yml`**
- Trigger: `paths: ['services/weather-forecast-service/**', 'kubernetes/weather-forecast-service-chart/**']`
- Build Context: `services/weather-forecast-service`
- Dockerfile: `services/weather-forecast-service/Dockerfile`

**Datei: `.github/workflows/deploy-travel-warnings.yml`**
- Trigger: `paths: ['services/travel-warnings-service/**', 'kubernetes/travel-warnings-service-chart/**']`
- Build Context: `services/travel-warnings-service`
- Dockerfile: `services/travel-warnings-service/Dockerfile`

### 2.2 Chart.yaml anpassen

Jeder Service braucht sein eigenes `kubernetes/<service>-chart/Chart.yaml`:
```yaml
apiVersion: v2
name: itinerary-service  # weather-service, travel-warnings-service
description: Itinerary Service Helm Chart
type: application
version: 1.0.0
appVersion: "1.0.0"
```

### 2.3 Versionierung

**Option A: Separate Versionen pro Service** (EMPFOHLEN)
- `kubernetes/itinerary-service-chart/Chart.yaml` → appVersion: 1.0.0
- `kubernetes/weather-service-chart/Chart.yaml` → appVersion: 1.0.0
- `kubernetes/travel-warnings-chart/Chart.yaml` → appVersion: 1.0.0

**Option B: Shared Version**
- Alle Services haben gleiche Version

## Phase 3: Docker & Registry (1 Tag)

### 3.1 Image Namen
```
europe-west1-docker.pkg.dev/PROJECT_ID/docker-repo/itinerary-service:1.0.0
europe-west1-docker.pkg.dev/PROJECT_ID/docker-repo/weather-service:1.0.0
europe-west1-docker.pkg.dev/PROJECT_ID/docker-repo/travel-warnings-service:1.0.0
```

### 3.2 Build Commands
```bash
# Itinerary
docker build -t europe-west1-docker.pkg.dev/PROJECT_ID/docker-repo/itinerary-service:1.0.0 \
  -f services/itinerary-service/Dockerfile \
  services/itinerary-service

# Weather
docker build -t europe-west1-docker.pkg.dev/PROJECT_ID/docker-repo/weather-service:1.0.0 \
  -f services/weather-forecast-service/Dockerfile \
  services/weather-forecast-service

# Travel Warnings
docker build -t europe-west1-docker.pkg.dev/PROJECT_ID/docker-repo/travel-warnings-service:1.0.0 \
  -f services/travel-warnings-service/Dockerfile \
  services/travel-warnings-service
```

## Phase 4: Testing (2-3 Tage)

### 4.1 Lokales Testing
```bash
# Build alle Services lokal
cd services/itinerary-service && docker build -t itinerary-service:test .
cd services/weather-forecast-service && docker build -t weather-service:test .
cd services/travel-warnings-service && docker build -t travel-warnings:test .

# docker-compose für lokales Testing
```

### 4.2 CI/CD Testing
- Trigger jeden Workflow einzeln
- Verify Docker Images werden gebaut
- Verify Helm Charts deployen korrekt

### 4.3 Deployment Testing
- Deploy auf Test/Staging Environment
- Smoke Tests
- Integration Tests

## Phase 5: Deployment & Rollout (1 Tag)

### 5.1 Deployment Reihenfolge
1. cert-manager (falls noch nicht deployed)
2. Datenbank-Services (falls vorhanden)
3. Backend Services (parallel oder sequenziell):
   - itinerary-service
   - weather-forecast-service
   - travel-warnings-service
4. Gateway/Ingress

### 5.2 Rollback Plan
- Alte Workflows behalten als Backup
- Helm Rollback Commands bereit
- Database Backups

## Phase 6: Cleanup & Documentation (1 Tag)

### 6.1 Alte Files löschen
```bash
rm -f Weather.Dockerfile TravelWarnings.Dockerfile
rm -rf weather-forecast-service/ travel_warnings/  # Nach Migration
```

### 6.2 Documentation Update
- README.md aktualisieren
- Architecture Diagramm
- Developer Setup Guide
- Deployment Guide

---

## Checkliste

### Pre-Migration
- [ ] Backup des gesamten Repos
- [ ] Alle Tests laufen
- [ ] Dokumentation der aktuellen Struktur

### Migration
- [ ] Neues Branch erstellt
- [ ] Ordnerstruktur umgebaut
- [ ] Pipelines angepasst
- [ ] Docker Builds getestet
- [ ] Kubernetes Charts angepasst
- [ ] Lokale Tests erfolgreich

### Post-Migration
- [ ] Production Deployment erfolgreich
- [ ] Monitoring funktioniert
- [ ] Alle Services erreichbar
- [ ] Documentation aktualisiert
- [ ] Team informiert

---

## Zeitplan

**Gesamt: 7-11 Arbeitstage**

| Phase | Dauer | Verantwortlich |
|-------|-------|---------------|
| Phase 1: Restructure | 1-2 Tage | Dev Team |
| Phase 2: Pipelines | 2-3 Tage | DevOps |
| Phase 3: Docker | 1 Tag | DevOps |
| Phase 4: Testing | 2-3 Tage | QA + Dev |
| Phase 5: Deployment | 1 Tag | DevOps |
| Phase 6: Cleanup | 1 Tag | Dev Team |

---

## Risiken & Mitigation

| Risiko | Wahrscheinlichkeit | Impact | Mitigation |
|--------|-------------------|--------|------------|
| Path-Dependencies brechen | Hoch | Hoch | Gründliche Tests, schrittweise Migration |
| Pipeline Failures | Mittel | Mittel | Alte Pipelines als Backup behalten |
| Deployment Issues | Mittel | Hoch | Blue-Green Deployment, Rollback Plan |
| Lost Configuration | Niedrig | Hoch | Git History, Backups |
