# Local Development Setup

This directory contains all the necessary configuration to run the Travel App backend locally with all required services.

## Services Included

1. **PostgreSQL Database** - Main relational database for users, itineraries, and locations
2. **Firestore Emulator** - NoSQL database emulator for likes and comments
3. **Google Cloud Storage Emulator** - Object storage emulator for images

## Prerequisites

- Docker and Docker Compose installed
- Java 21 installed
- Maven installed

## Quick Start

### 1. Start all services

```bash
cd local-setup
docker-compose up -d
```

This will start:
- PostgreSQL on port `5432`
- Firestore Emulator on port `8081`
- GCS Emulator on port `4443`

### 2. Verify services are running

```bash
docker-compose ps
```

All services should show as "Up" (healthy).

### 3. Initialize GCS Bucket

The GCS emulator needs a bucket to be created. Run the initialization script:

```bash
./init-gcs-bucket.sh
```

Or manually create it:

```bash
curl -X POST http://localhost:4443/storage/v1/b \
  -H "Content-Type: application/json" \
  -d '{"name":"tripico-images"}'
```
### 4. Login to Google Cloud for Goodle Identity usage

````bash
gcloud auth application-default login
````



### 5. Run the application

From the project root:

```bash
mvn quarkus:dev -Dquarkus.profile=local
```

The application will start on `http://localhost:8080`

## Service Details

### PostgreSQL
- **Host**: localhost
- **Port**: 5432
- **Database**: mydatabase
- **Username**: myuser
- **Password**: mypassword

### Firestore Emulator
- **Host**: localhost
- **Port**: 8081
- **Project ID**: local-project
- **UI**: Not available (command-line only)

### Google Cloud Storage Emulator
- **Host**: localhost
- **Port**: 4443
- **Endpoint**: http://localhost:4443
- **Bucket**: tripico-images

## Configuration Profiles

The application uses Quarkus profiles for different environments:

- **local**: For local development (default when using docker-compose)
- **test**: For running tests
- **prod**: For production deployment

### Using Different Profiles

```bash
# Local development (default)
mvn quarkus:dev -Dquarkus.profile=local

# Production mode
mvn quarkus:dev -Dquarkus.profile=prod
```

## Environment Variables

For local development, these are already configured with sensible defaults in `application-local.yaml`:

```bash
PROJECT_ID=local-project
DB_URL=jdbc:postgresql://localhost:5432/mydatabase
DB_USER=myuser
DB_PASSWORD=mypassword
FIRESTORE_EMULATOR_HOST=localhost:8081
STORAGE_EMULATOR_HOST=localhost:4443
BUCKET_NAME=tripico-images
USE_FIRESTORE_EMULATOR=true
USE_STORAGE_EMULATOR=true
```

## Accessing Services

### Application APIs
- **REST API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/q/swagger-ui
- **OpenAPI Spec**: http://localhost:8080/q/openapi

### Database
Connect to PostgreSQL using any database client:
```bash
psql -h localhost -p 5432 -U myuser -d mydatabase
```

### Storage
List files in GCS emulator:
```bash
curl http://localhost:4443/storage/v1/b/tripico-images/o
```

## Testing the Setup

### 1. Register a User
```bash
curl -X POST http://localhost:8080/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@example.com"
  }'
```

### 2. Upload a Profile Image
```bash
curl -X POST http://localhost:8080/user/john.doe@example.com/profile-image \
  -F "file=@/path/to/image.jpg"
```

### 3. Create an Itinerary
```bash
curl -X POST http://localhost:8080/itinerary/create/john.doe@example.com \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Summer Vacation",
    "destination": "Norway",
    "startDate": "2024-06-15",
    "shortDescription": "Exploring fjords",
    "detailedDescription": "A wonderful trip through Norway"
  }'
```

### 4. Like an Itinerary
```bash
curl -X POST http://localhost:8080/itinerary/1/like \
  -H "Content-Type: application/json" \
  -d '{
    "userEmail": "jane.doe@example.com",
    "comment": "Looks amazing!"
  }'
```

## Troubleshooting

### Services won't start

Check if ports are already in use:
```bash
lsof -i :5432  # PostgreSQL
lsof -i :8081  # Firestore Emulator
lsof -i :4443  # GCS Emulator
```

### Firestore Emulator connection issues

Make sure the emulator is running and accessible:
```bash
curl http://localhost:8081
```

### GCS Emulator bucket not found

Re-run the bucket initialization:
```bash
./init-gcs-bucket.sh
```

### Database schema issues

If you need to reset the database:
```bash
docker-compose down -v  # Remove volumes
docker-compose up -d    # Restart services
```

## Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove all data
docker-compose down -v
```

## Production Deployment

For production deployment, use the `prod` profile and set these environment variables:

```bash
PROJECT_ID=your-gcp-project-id
DB_URL=jdbc:postgresql://your-db-host:5432/your-database
DB_USER=your-db-user
DB_PASSWORD=your-db-password
BUCKET_NAME=your-bucket-name
USE_FIRESTORE_EMULATOR=false
USE_STORAGE_EMULATOR=false
```

## Additional Resources

- [Quarkus Documentation](https://quarkus.io/guides/)
- [Google Cloud Firestore](https://cloud.google.com/firestore/docs)
- [Google Cloud Storage](https://cloud.google.com/storage/docs)

