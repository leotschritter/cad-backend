# Kubernetes Deployment

Diese Anleitung beschreibt, wie der Weather Forecast Service in Kubernetes deployt wird.

## Voraussetzungen

- Docker Image gebaut und verfügbar
- Kubernetes Cluster läuft (minikube, kind, GKE, etc.)
- kubectl konfiguriert

## Deployment Schritte

### 1. Docker Image bauen

```bash
cd weather-forecast-service
docker build -t weather-forecast-service:latest .
```

Für minikube das Image laden:
```bash
minikube image load weather-forecast-service:latest
```

### 2. Secret erstellen

Kopiere die example Datei und füge deine echten Credentials ein:
```bash
cp k8s/secret.yaml.example k8s/secret.yaml
# Bearbeite k8s/secret.yaml und setze die echten Werte
```

Oder erstelle das Secret direkt mit kubectl:
```bash
kubectl create secret generic weather-forecast-secret \
  --from-literal=db-username=YOUR_DB_USERNAME \
  --from-literal=db-password=YOUR_DB_PASSWORD \
  --from-literal=meteosource-api-key=YOUR_METEOSOURCE_API_KEY
```

### 3. ConfigMap erstellen

Passe die ConfigMap an deine Umgebung an (z.B. Database URL):
```bash
# Bearbeite k8s/configmap.yaml und passe die db-jdbc-url an
kubectl apply -f k8s/configmap.yaml
```

### 4. Secret anwenden

```bash
kubectl apply -f k8s/secret.yaml
```

### 5. Service deployen

```bash
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

### 6. Deployment überprüfen

```bash
# Pods anzeigen
kubectl get pods -l app=weather-forecast-service

# Logs anzeigen
kubectl logs -l app=weather-forecast-service -f

# Service anzeigen
kubectl get svc weather-forecast-service

# Health Check testen
kubectl port-forward svc/weather-forecast-service 8080:8080
curl http://localhost:8080/q/health
```

## Kubernetes Features

### Health Checks

Der Service implementiert Kubernetes Health Probes:

- **Liveness Probe**: `/q/health/live`
  - Prüft ob die Anwendung läuft
  - Bei Fehlschlag wird der Pod neu gestartet

- **Readiness Probe**: `/q/health/ready`
  - Prüft ob die Anwendung bereit ist Requests zu empfangen
  - Bei Fehlschlag werden keine Requests an den Pod weitergeleitet

### Resource Limits

Das Deployment definiert Resource Requests und Limits:
- Requests: 512Mi RAM, 250m CPU
- Limits: 1Gi RAM, 1000m CPU

### Replicas

Standardmäßig werden 2 Replicas deployt für High Availability.

## Troubleshooting

### Pod startet nicht

```bash
kubectl describe pod <pod-name>
kubectl logs <pod-name>
```

### Health Check schlägt fehl

```bash
# In den Pod einloggen
kubectl exec -it <pod-name> -- /bin/bash

# Health Endpoint testen
curl localhost:8080/q/health/live
curl localhost:8080/q/health/ready
```

### ConfigMap/Secret Änderungen

Nach Änderungen an ConfigMap oder Secret müssen die Pods neu gestartet werden:
```bash
kubectl rollout restart deployment/weather-forecast-service
```

## Cleanup


```bash
kubectl delete -f k8s/deployment.yaml
kubectl delete -f k8s/service.yaml
kubectl delete -f k8s/configmap.yaml
kubectl delete -f k8s/secret.yaml
```
