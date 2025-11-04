# Travel App Backend

A Quarkus-based REST API for managing travel itineraries and user accounts. This application provides endpoints for user registration and itinerary management with PostgreSQL database integration.

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- gcloud CLI

### Local Development Setup

**The easiest way to get started:**

```bash
cd local-setup
./start-local-dev.sh
```

This will start:
- PostgreSQL database (port 5432)
- Firestore emulator (port 8081)
- Google Cloud Storage emulator (port 4443)

Then login to Google Cloud for Identity usage:

```bash
gcloud auth application-default login
```
Then run the application:


```bash
mvn quarkus:dev -Dquarkus.profile=local
```

**For detailed setup instructions, see:**
- [Complete Setup Guide](local-setup/README.md) - Detailed documentation

### Manual Setup

#### 1. Start All Services
First, start all required services using Docker Compose:

```bash
cd local-setup
docker-compose up -d
./init-gcs-bucket.sh
```

This will start:
- PostgreSQL 16 (localhost:5432)
- Firestore Emulator (localhost:8081)
- Google Cloud Storage Emulator (localhost:4443)

#### 2. Run the Application
In the project root directory, start the Quarkus application:

```bash
mvn quarkus:dev
```

The application will start on `http://localhost:8080`

#### 3. Access the API Documentation
Once the application is running, you can access:

- **Swagger UI**: [http://localhost:8080/q/swagger-ui/](http://localhost:8080/q/swagger-ui/)
- **OpenAPI YAML**: [http://localhost:8080/q/openapi](http://localhost:8080/q/openapi)
- **Quarkus Dev UI**: [http://localhost:8080/q/dev/](http://localhost:8080/q/dev/)

## 📚 API Endpoints

### User Management
- **POST** `/user/register` - Register a new user
- **GET** `/user/get?email={email}` - Get user by email

### Itinerary Management
- **POST** `/itinerary/create?userId={userId}` - Create a new itinerary
- **GET** `/itinerary/get?userId={userId}` - Get user's itineraries

## 🗄️ Database Schema

The application uses PostgreSQL with the following entities:

### User Entity
- `id` (Long, Primary Key)
- `name` (String)
- `email` (String, Unique)
- `itineraryList` (List<Itinerary>, One-to-Many)

### Itinerary Entity
- `id` (Long, Primary Key)
- `title` (String)
- `shortDescription` (String)
- `destination` (String)
- `detailedDescription` (String)
- `startDate` (LocalDate)
- `user` (User, Many-to-One)

## 🧪 Testing

Run the test suite:

```bash
mvn test
```

The project includes comprehensive unit tests using Mockito for service layer testing.

## 📦 Building and Packaging

### Development Mode
```bash
mvn quarkus:dev
```

### Package Application
```bash
mvn package
```

### Create Native Executable
```bash
mvn package -Dnative
```

## 🛠️ Development

### Hot Reload
The application supports hot reload in development mode. Changes to Java files will automatically restart the application.

### Database Migrations
Hibernate ORM is configured with `database.generation: update`, which automatically creates/updates database schema based on entity definitions.

### Logging
SQL queries are logged in development mode for debugging purposes.

## 🐳 Docker Support

The project includes Docker Compose configuration for local development:

```bash
# Start database
cd local-setup
docker-compose up -d

# Stop database
docker-compose down
```

## ☁️ Cloud Deployment

### Terraform Infrastructure as Code

The project includes comprehensive Terraform configuration for automated deployment to Google Cloud Platform.

**Quick Start:**

```bash
cd terraform
terraform init
terraform plan
terraform apply
```

The Terraform setup creates:
- Cloud Run service
- Cloud SQL (PostgreSQL 16)
- Firestore database
- Cloud Storage bucket
- Artifact Registry
- Service accounts with IAM bindings
- Secret Manager for credentials

**For detailed instructions, see:**
- [Terraform Deployment Guide](terraform22/README.md) - Complete Terraform documentation

### Manual Deployment (gcloud)

For manual deployment using gcloud commands, see `gcloud_deployment.sh`.

## 📖 Documentation

- **Quarkus Documentation**: [https://quarkus.io/](https://quarkus.io/)
- **OpenAPI Specification**: Available at `/q/openapi` when running
- **Swagger UI**: Available at `/q/swagger-ui/` when running

## Generate `openapi.yaml`
```bash
curl -s http://localhost:8080/q/openapi > openapi.yaml
```

## 📄 License

This project is part of a university course assignment.
