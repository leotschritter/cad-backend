# Travel App Backend

A Quarkus-based REST API for managing travel itineraries and user accounts. This application provides endpoints for user registration and itinerary management with PostgreSQL database integration.

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose

### Local Development Setup

#### 1. Start the Database
First, start the PostgreSQL database using Docker Compose:

```bash
cd local-setup
docker-compose up -d
```

This will start a PostgreSQL 16 container with the following configuration:
- **Host**: localhost:5432
- **Database**: mydatabase
- **Username**: myuser
- **Password**: mypassword

#### 2. Run the Application
In the project root directory, start the Quarkus application in development mode:

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
