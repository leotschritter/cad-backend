# Travel Warnings Microservice

A comprehensive travel warning microservice that integrates with the Auswärtiges Amt (German Federal Foreign Office) API to provide real-time travel safety information and alerts.

## 🎯 User Stories Implemented

### ✅ User Story 1: Receive Timely, Relevant Safety Alerts
- **Feature**: Automated notification system that alerts travelers only for countries in their itinerary
- **Implementation**: 
  - Trip-based alert matching
  - Configurable notification preferences per trip
  - Email alerts sent only when warnings affect active/upcoming trips
  - Severity levels clearly indicated (None, Minor, Moderate, Severe, Critical)

### ✅ User Story 2: Understand Safety Changes Quickly
- **Feature**: Clear, concise summaries of travel warnings
- **Implementation**:
  - HTML-formatted email alerts with structured information
  - Summary highlights: what changed, severity, recommended actions
  - Visual severity indicators with color coding
  - Scannable format with sections and bullet points

### ✅ User Story 3: Access Comprehensive Safety Information
- **Feature**: Detailed, organized safety information by category
- **Implementation**:
  - Content categorization: Security, Nature & Climate, Travel Info, Documents & Customs, Health
  - Full content access via REST API
  - Links to official Auswärtiges Amt pages
  - Both summary and detailed views available

## 🏗️ Architecture

### Technology Stack
- **Framework**: Quarkus 3.29.3 (Java 21)
- **Database**: PostgreSQL 15
- **ORM**: Hibernate with Panache
- **REST Client**: MicroProfile REST Client
- **Email**: Quarkus Mailer (configurable for SendGrid)
- **Caching**: Caffeine Cache
- **Scheduling**: Quarkus Scheduler
- **API Documentation**: SmallRye OpenAPI (Swagger UI)

### Components (Based on Bounded Context Diagram)
1. **Warning Fetcher**: Polls Auswärtiges Amt API every 15 minutes
2. **Warning Matcher**: Matches warnings with user trips based on country and dates
3. **Alert Dispatcher**: Sends email notifications to affected users

## 🚀 Getting Started

### Prerequisites
- Java 21 or higher
- Maven 3.8+
- Docker & Docker Compose (for PostgreSQL)

### 1. Start the Database
```cmd
docker-compose up -d
```

This starts:
- PostgreSQL on port 5432
- pgAdmin on port 5050 (http://localhost:5050)

### 2. Configure Environment Variables

Create a `.env` file or set environment variables (see `.env.example`):

```properties
# SendGrid Email Configuration (optional)
SMTP_FROM=<username>@htwg-konstanz.de
SMTP_HOST=smtp.htwg-konstanz.de
SMTP_PORT=587
SMTP_USER=<username>
SMTP_PASSWORD=<htwg-password>
SMTP_START_TLS=REQUIRED

```

### 3. Run the Application

**Development Mode (with live reload):**
```cmd
mvnw quarkus:dev
```

**Production Mode:**
```cmd
mvnw clean package
java -jar target\quarkus-app\quarkus-run.jar
```

### 4. Access the Application

- **API Base URL**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui
- **OpenAPI Spec**: http://localhost:8080/openapi
- **Health Check**: http://localhost:8080/q/health

## 🔄 Automated Warning Polling

The service automatically polls the Auswärtiges Amt API every 15 minutes (configurable):

```properties
# application.properties
travel-warnings.poll.cron=0 */15 * * * ?
```

**Process Flow:**
1. Fetch latest warnings from Auswärtiges Amt API
2. Compare with existing database entries (using lastModified timestamp)
3. Update changed warnings
4. Match warnings with active/upcoming user trips
5. Send email alerts (only if not already sent for this version)

## 📧 Email Notifications

### Configuration

The service uses Quarkus Mailer.

### Email Content

Emails include:
- **Severity-coded header** with visual indicators
- **Trip information**: name, destination, dates
- **Alert summary**: what's new, severity, status
- **Warning details**: full/partial warnings
- **Recommended actions**: specific guidance based on severity
- **Links**: to official Auswärtiges Amt page

## 🗄️ Database Schema

### Main Tables

**travel_warnings**
- Stores fetched travel warnings from Auswärtiges Amt
- Indexed on: country_code, content_id, last_modified

**user_trips**
- Stores user trip itineraries
- Indexed on: email, country_code, travel_dates

**warning_notifications**
- Tracks sent notifications to prevent duplicates
- Indexed on: user_trip_id, warning_content_id, sent_at

## 🧪 Testing

### Run Tests
```cmd
mvnw test
```

### Run with Coverage
```cmd
mvnw test jacoco:report
```

### Integration Tests
The project includes integration tests using Quarkus Test framework and RestAssured.

## 🔧 Configuration

### Key Configuration Properties

```properties
# Database
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/travel_warnings_db
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres

# External API
quarkus.rest-client."de.htwg.travelwarnings.client.AuswaertigesAmtClient".url=https://www.auswaertiges-amt.de/opendata

# Scheduler
travel-warnings.poll.cron=0 */15 * * * ?

# Cache
quarkus.cache.caffeine."travel-warnings".expire-after-write=PT15M
```

## 📊 Monitoring & Health

### Health Endpoints
- **Readiness**: http://localhost:8080/q/health/ready
- **Liveness**: http://localhost:8080/q/health/live

### Metrics
- Database connection status
- Number of warnings stored
- Service availability

## 🐳 Docker Deployment

### Build Native Image
```cmd
mvnw package -Pnative -Dquarkus.native.container-build=true
```

### Run with Docker
```cmd
docker build -f src/main/docker/Dockerfile.jvm -t travel-warnings-service .
docker run -p 8080:8080 travel-warnings-service
```

## 🤝 Contributing

This is a university project for the CAD course at HTWG Konstanz.

## 📄 License

This project is for educational purposes.

## 🔗 References

- [Auswärtiges Amt OpenData API](https://www.auswaertiges-amt.de/de/open-data-schnittstelle/736118)
- [Quarkus Documentation](https://quarkus.io/guides/)
