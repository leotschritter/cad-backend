# Travel App Backend

A Quarkus-based REST API for managing travel itineraries and user accounts. This application provides endpoints for user registration and itinerary management with PostgreSQL database integration.
## Load Test Results Summary

This suite has been tested against the deployed Cloud Run backend (`https://api.tripico.fun`) with the following configurations:

### Periodic Workload Test (Normal Traffic)
- **Users**: 20 concurrent users
- **Duration**: 10 minutes
- **Ramp-up**: 2 users/second (10 seconds to full load)
- **Result**: **Stable** - The application handled normal daily traffic smoothly with minimal failed requests (<1% failure rate). Average response times remained consistent throughout the test.

### Once-in-a-Lifetime Workload Test (Traffic Spike)
- **Users**: 5,000 concurrent users
- **Duration**: 10 minutes  
- **Ramp-up**: 50 users/second (100 seconds to full load)
- **Result**: **Struggled under peak load** - The application experienced significant performance degradation under extreme traffic. With Cloud Run's limit of 10 instances, the service was overwhelmed by the burst of requests, resulting in increased response times, timeouts, and failed requests. This test successfully identified the system's breaking point at approximately 2,000-3,000 concurrent users.

### Key Findings
- **Normal operations**: The application performs well under typical load conditions
- **Scaling limits**: Current infrastructure caps at ~10 instances (20GB total memory), which limits capacity during traffic spikes
- **Recommendation**: For handling viral traffic scenarios, consider increasing `cloud_run_max_instances` or implementing request throttling/queuing mechanisms

### Potential Bottlenecks
Based on the architecture and observed behavior, the following components may contribute to performance degradation under high load:

- **Firebase Authentication**: Each request requires token verification against Firebase Auth. Under heavy load, the authentication layer may introduce latency, especially when thousands of users authenticate simultaneously. Token caching could mitigate this.

- **PostgreSQL Database**: The shared `db-f1-micro` instance has limited CPU and memory resources. Complex queries (itinerary searches, joins with locations) may slow down under concurrent load. Connection pool exhaustion could also occur if many Cloud Run instances compete for database connections.

- **Firestore Operations**: Likes and comments are stored in Firestore. While Firestore scales well, the overhead of network calls to Firestore from each Cloud Run instance adds latency. Batch operations or caching frequently accessed data could improve performance.

- **Cloud Run Instance Limits**: The hard cap of 10 instances creates a ceiling for concurrent request handling. Each instance handles ~200-500 requests depending on endpoint complexity and response time.

### Next Steps for Deeper Analysis

To identify the exact bottleneck and determine capacity thresholds, consider the following approaches:

#### 1. Fine-Tune Ramp-Up Testing
Adjust the load test configuration in `load/.env.onceinlifetime` to find the breaking point

Run multiple tests with incremental user counts (1000 → 2000 → 3000 → 4000) to identify at which point:
- Response times start degrading (>500ms)
- Error rates increase (>5%)
- Instances hit the maximum limit
---

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
- [Terraform Deployment Guide](terraform/README.md) - Complete Terraform documentation

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
