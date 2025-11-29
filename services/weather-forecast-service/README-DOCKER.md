# Weather Forecast Service - Docker Setup

## Architektur

Der Weather Forecast Service ist ein eigenständiger Microservice mit:
- **Quarkus** Framework (Java 21)
- **PostgreSQL** Datenbank (Port 5433)
- **REST API** (Port 8081)

## Schnellstart

### 1. Service starten

```bash
cd weather-forecast-service
docker-compose up -d
```

### 2. Service stoppen

```bash
docker-compose down
```

### 3. Mit Datenbank-Löschung stoppen

```bash
docker-compose down -v
```

## Zugriff

- **API**: http://localhost:8081
- **Swagger UI**: http://localhost:8081/swagger-ui
- **Health Check**: http://localhost:8081/q/health

## Datenbank-Zugriff

```bash
# In Container einloggen
docker exec -it weather-forecast-db psql -U weather_user -d weather_forecast

# Von Host (PostgreSQL Client benötigt)
psql -h localhost -p 5433 -U weather_user -d weather_forecast
```

## Entwicklung

### Lokale Entwicklung ohne Docker

```bash
# PostgreSQL Datenbank starten
docker-compose up -d weather-db

# Quarkus Dev Mode
./mvnw quarkus:dev
```

Die Anwendung läuft dann auf Port 8080 und verbindet sich mit der Datenbank auf Port 5433.

### Logs anzeigen

```bash
# Alle Logs
docker-compose logs -f

# Nur Service Logs
docker-compose logs -f weather-service

# Nur Datenbank Logs
docker-compose logs -f weather-db
```

### Rebuild nach Code-Änderungen

```bash
docker-compose up -d --build
```

## Troubleshooting

### Port bereits belegt

Falls Port 8081 oder 5433 bereits belegt sind, ändern Sie die Ports in `docker-compose.yml`:

```yaml
services:
  weather-db:
    ports:
      - "5434:5432"  # Ändern Sie 5433 zu 5434

  weather-service:
    ports:
      - "8082:8080"  # Ändern Sie 8081 zu 8082
```

### Container startet nicht

```bash
# Status prüfen
docker-compose ps

# Logs prüfen
docker-compose logs weather-service

# Container neu bauen
docker-compose build --no-cache
docker-compose up -d
```

### Datenbank zurücksetzen

```bash
docker-compose down -v
docker-compose up -d
```

## Umgebungsvariablen

Erstellen Sie eine `.env` Datei basierend auf `.env.example` für custom Konfiguration:

```bash
cp .env.example .env
# Bearbeiten Sie .env nach Bedarf
```

## Integration mit Hauptanwendung

Wenn Sie diesen Service mit dem Haupt-Backend verbinden möchten:

1. Fügen Sie das `weather-network` zum Haupt-`docker-compose.yml` hinzu
2. Oder verwenden Sie einen API Gateway / Service Mesh
3. Konfigurieren Sie Service Discovery (z.B. Consul, Eureka)