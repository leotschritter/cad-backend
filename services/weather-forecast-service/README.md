# Weather Forecast Service

Ein Microservice für Wettervorhersagen, der die Meteosource API nutzt, um Wetterinformationen abzurufen und zu speichern.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

## Features

- Integration mit Meteosource Weather API
- Speicherung von Wettervorhersagen in PostgreSQL
- RESTful API Endpunkte für Wetterabfragen
- **Tägliche Wettervorhersagen** (7 Tage) mit detaillierten Informationen:
  - Temperatur (hoch/niedrig)
  - Wetterbedingungen und Icons
  - Niederschlagswahrscheinlichkeit und Gesamtniederschlag
  - Windgeschwindigkeit und -richtung
  - Luftfeuchtigkeit und UV-Index
  - Zusammenfassung
- **Stündliche Wettervorhersagen** (24 Stunden) mit:
  - Aktuelle Temperatur
  - Wetterbedingungen und Icons
  - Windgeschwindigkeit und -richtung
  - Niederschlagsmenge und -typ
  - Wolkenbedeckung
  - Zusammenfassung

## Prerequisites

- Java 21
- Maven 3.8+
- PostgreSQL 15+
- Meteosource API Key (kostenlos erhältlich unter https://www.meteosource.com/)

## Configuration

### Sicherheitshinweis ⚠️

**WICHTIG:** Niemals echte API-Keys, Passwörter oder andere Secrets in `application.properties` committen!

Alle sensiblen Daten werden über Umgebungsvariablen gesetzt:
- **Lokal:** `.env` Datei (bereits in `.gitignore`)
- **Docker:** `.env` Datei + `docker-compose.yml`
- **Production:** Umgebungsvariablen (Kubernetes Secrets, Cloud Secret Manager, etc.)

### Setup

1. **Kopiere das Template und fülle die Werte aus:**
   ```bash
   cp .env.example .env
   ```

2. **Bearbeite die `.env` Datei und setze deine echten Werte:**
   ```bash
   # Wichtig: Ändere diese Werte!
   POSTGRES_PASSWORD=dein-sicheres-passwort
   METEOSOURCE_API_KEY=dein-echter-api-key
   ```

3. **Verifiziere, dass `.env` in `.gitignore` steht** (bereits konfiguriert)

### Umgebungsvariablen

| Variable | Beschreibung | Beispiel |
|----------|--------------|----------|
| `POSTGRES_DB` | Datenbankname | `weather_forecast` |
| `POSTGRES_USER` | Datenbankbenutzer | `weather_user` |
| `POSTGRES_PASSWORD` | Datenbankpasswort | `secure_pass_123` |
| `QUARKUS_DATASOURCE_JDBC_URL` | JDBC URL | `jdbc:postgresql://localhost:5433/weather_forecast` |
| `METEOSOURCE_API_KEY` | Meteosource API Key | Hol dir einen auf https://www.meteosource.com/ |

## API Endpoints

### Wettervorhersage abrufen und speichern

#### Nach Koordinaten
```http
POST /api/weather/forecast/coordinates?lat=48.1351&lon=11.5820&location=Munich
```

#### Nach Ort-ID
```http
POST /api/weather/forecast/place?placeId=munich
```

### Gespeicherte Wettervorhersagen abrufen

#### Nach Standort
```http
GET /api/weather/forecast/location/Munich
```

#### Nach Koordinaten
```http
GET /api/weather/forecast/coordinates?lat=48.1351&lon=11.5820
```

#### Alle Vorhersagen
```http
GET /api/weather/forecasts
```

### Beispiel Response

```json
{
  "id": 1,
  "location": "Munich",
  "latitude": 48.1351,
  "longitude": 11.5820,
  "timezone": "Europe/Berlin",
  "lastUpdated": "2025-01-14T10:30:00",
  "dailyForecasts": [
    {
      "date": "2025-01-14",
      "temperatureHigh": 8.5,
      "temperatureLow": 2.3,
      "weather": "Partly cloudy",
      "weatherIcon": "partly_cloudy",
      "precipitationProbability": 20,
      "precipitationTotal": 0.0,
      "windSpeed": 12.5,
      "windDirection": 270,
      "humidity": 75,
      "uvIndex": 2,
      "summary": "Partly cloudy throughout the day"
    }
  ]
}
```

## Architecture

```
weather-forecast-service/
├── src/main/java/de/htwg/
│   ├── api/              # REST Endpunkte
│   ├── client/           # External API Clients (Meteosource)
│   ├── domain/           # JPA Entities
│   ├── dto/              # Data Transfer Objects
│   ├── persistence/      # Repositories
│   └── service/          # Business Logic
```

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/weather-forecast-service-1.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
