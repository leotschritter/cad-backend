# Cloud Native Project - Cloud Application Development – Winter Term 2025/26
## Team Bermuda Dreieck

**Team Members:** Leo Tschritter, Dennis Hoang, Benedikt Scheffel

**Date:** November 30, 2025

---

## Table of Contents
1. [Requirements](#1-requirements)
   - 1.1 [System Context](#11-system-context)
   - 1.2 [Feature Overview](#12-feature-overview)
2. [Development View](#2-development-view)
   - 2.1 [Software Components](#21-software-components)
   - 2.2 [Data Model](#22-data-model)
3. [Runtime View](#3-runtime-view)
   - 3.1 [Runtime Overview](#31-runtime-overview)
   - 3.2 [Microservices](#32-microservices)
   - 3.3 [Datastores](#33-datastores)
4. [DevOps](#4-devops)
   - 4.1 [IaC](#41-iac)
5. [Performance Tests](#5-performance-tests)
   - 5.1 [Periodic Workload](#51-periodic-workload)
   - 5.2 [Once-in-a-lifetime Workload](#52-once-in-a-lifetime-workload)

---

## 1 Requirements

### Introduction

**Tripico** is a cloud-native travel planning application that enables users to create, share, and discover travel itineraries. The system provides comprehensive travel planning features including itinerary management, personalized recommendations, real-time weather forecasts, travel warnings, and social interactions through comments and likes.

The application is built as a microservices architecture deployed on Google Cloud Platform (GCP), utilizing Kubernetes (GKE) for orchestration, Cloud SQL for relational data, Firestore for document storage, and Neo4j for graph-based recommendations.

### 1.1 System Context

[//]: # (# TODO: Add System Context Diagram here showing Tripico and all neighboring systems)

The Tripico application interacts with the following external systems and actors:

#### External Systems & APIs
- **Auswärtiges Amt API** - German Federal Foreign Office API providing real-time travel warnings and safety information for countries worldwide
- **Meteosource Weather API** - Weather forecast service providing 7-day daily forecasts and 24-hour hourly forecasts for travel destinations
- **Firebase Authentication** - Identity platform for user authentication and authorization across all microservices

#### Actors
- **Travelers (End Users)** - Primary users who create itineraries, browse recommendations, interact with content, and receive travel alerts

#### User Interfaces
- **Web Frontend** - Vue.js/Vuetify-based single-page application (https://frontend.tripico.fun)
- **REST APIs** - OpenAPI-documented endpoints for all microservices

### 1.2 Feature Overview

#### Core Features

**1. Itinerary Management**
- Create, read, update, and delete travel itineraries
- Add locations, accommodations, and transport details to trips
- Search and filter itineraries by various criteria

**2. Personalized Recommendations (Graph-based)**
- Recommendation feed using Neo4j graph database
- Collaborative filtering: "Users who liked what you liked also liked..."
- Location-based recommendations based on visited destinations
- Social signals integration (popularity based on likes count)
- Automatic fallback to popular content for new users

**3. Travel Warnings & Alerts**
- Real-time travel warning information from Auswärtiges Amt
- Automated notification system for trips affected by new warnings
- Email alerts with severity levels (None, Minor, Moderate, Severe, Critical)
- Trip-based alert matching with configurable preferences
- Categorized safety information (Security, Nature & Climate, Travel Info, Documents, Health)

**4. Weather Forecasts**
- 7-day daily weather forecasts for travel destinations
- 24-hour hourly forecasts with detailed metrics
- Temperature, precipitation, wind, humidity, and UV index
- Weather condition icons and summaries
- Automatic caching and periodic updates

**5. Social Interactions**
- Like and unlike itineraries
- Comment on itineraries with threaded discussions
- View likes count and engagement metrics

---

## 2 Development View

### 2.1 Software Components

#### Repository Organization

**Backend Monorepo:**
- Repository: https://github.com/leotschritter/cad-backend
- Structure: Microservices organized in `/services/` directory
- Infrastructure: Terraform IaC in `/terraform/` and service-specific `terraform/` directories
- Load Testing: Comprehensive test suite in `/load/` directory

**Frontend Repository:**
- Repository: https://github.com/leotschritter/cad-frontend
- Technology: Vue.js 3 with Vuetify UI framework
- Deployment: Static site hosted on GCP

#### Software Components Overview

The application consists of five independent microservices:

| Component | Programming Language | Framework | Version | Purpose |
|-----------|---------------------|-----------|---------|---------|
| **Itinerary Service** | Java 21 | Quarkus 3.28.1 | 1.0.0-SNAPSHOT | Core service for itinerary and user management |
| **Recommendation Service** | Java 21 | Quarkus 3.x | 1.0.0-SNAPSHOT | Graph-based recommendation engine |
| **Comments & Likes Service** | Java 21 | Quarkus 3.x | 1.0.0-SNAPSHOT | Social interaction features |
| **Travel Warnings Service** | Java 21 | Quarkus 3.29.3 | 1.0.0-SNAPSHOT | Travel safety alerts and notifications |
| **Weather Forecast Service** | Java 21 | Quarkus 3.x | 1.0.0-SNAPSHOT | Weather data integration |
| **Frontend Application** | JavaScript/TypeScript | Vue.js 3 + Vuetify | - | User interface |

#### Key Libraries & Dependencies

**Backend Services (Common):**
- **Quarkus REST (formerly RESTEasy Reactive)** - RESTful web services
- **Quarkus Jackson** - JSON serialization/deserialization
- **Hibernate ORM with Panache** - JPA-based data persistence (for PostgreSQL services)
- **SmallRye OpenAPI** - API documentation (Swagger UI)
- **SmallRye Health** - Health check endpoints
- **MapStruct 1.6.3** - DTO to entity mapping
- **Lombok 1.18.38** - Boilerplate code reduction
- **Firebase Admin SDK** - Authentication and authorization
- **JUnit 5 + Mockito 5.14.2** - Unit and integration testing

**Service-Specific Libraries:**
- **Itinerary Service:**
  - PostgreSQL JDBC Driver
  - Google Cloud SQL connector 1.19.1
  - Google Cloud Storage (for future file uploads)
  
- **Recommendation Service:**
  - Neo4j Java Driver 5.x - Graph database connectivity
  - Custom graph traversal algorithms for collaborative filtering
  
- **Comments & Likes Service:**
  - Google Cloud Firestore SDK - NoSQL document database
  - Firebase Admin SDK for authentication
  
- **Travel Warnings Service:**
  - Quarkus Mailer - Email notifications
  - Quarkus REST Client - External API integration
  - Caffeine Cache - API response caching
  - Quarkus Scheduler - Periodic warning checks (every 15 minutes)
  
- **Weather Forecast Service:**
  - PostgreSQL JDBC Driver
  - Quarkus REST Client - Meteosource API integration
  - Quarkus Scheduler - Daily forecast updates

**Frontend:**
- Vue.js 3 - Progressive JavaScript framework
- Vuetify 3 - Material Design component library
- Vue Router - Client-side routing
- Axios/Fetch API - HTTP client for backend communication
- Firebase Authentication SDK - User authentication

#### External Interfaces (OpenAPI Specifications)

All microservices expose RESTful APIs documented with OpenAPI 3.0:

- **Itinerary Service API:** `/services/itinerary-service/openapi.yaml`
  - User management endpoints (`/api/users`)
  - Itinerary CRUD endpoints (`/api/itineraries`)
  - Location management (`/api/locations`)
  - Search and filtering capabilities
  
- **Recommendation Service API:** `/services/recommendation-service/openapi.yaml`
  - Personalized feed endpoint (`/api/recommendations/feed`)
  - Graph synchronization endpoints (internal)
  - User preference management
  
- **Comments & Likes Service API:** `/services/comments-likes-service/openapi.yaml`
  - Comments CRUD (`/api/comments`)
  - Likes management (`/api/likes`)
  - Engagement metrics
  
- **Travel Warnings Service API:** `/services/travel_warnings/openapi.yaml`
  - Travel warnings retrieval (`/api/warnings`)
  - User trip registration (`/api/trips`)
  - Notification preferences
  
- **Weather Forecast Service API:** `/services/weather-forecast-service/openapi.yaml`
  - Daily forecasts (`/api/weather/daily`)
  - Hourly forecasts (`/api/weather/hourly`)
  - Location-based weather queries

### 2.2 Data Model

#### Persistent Storage Overview

The application uses multiple database technologies optimized for different data types:

| Storage Type | Technology | Services Using It | Purpose |
|--------------|------------|-------------------|---------|
| **Relational Database** | PostgreSQL 15 | Itinerary, Weather Forecast, Travel Warnings | Structured data with complex relationships |
| **Document Database** | Google Firestore | Comments & Likes | Flexible schema for social interactions |
| **Graph Database** | Neo4j 5.x | Recommendation | Graph-based relationship modeling |
| **Cloud Storage** | Google Cloud Storage | Itinerary (future) | File storage (planned feature) |

---

#### Itinerary Service - PostgreSQL Database

[//]: # (# TODO: Add ER Diagram for Itinerary Service database schema)

**Entities:**

**User**
- `id` (UUID, PK) - Unique user identifier
- `firebase_uid` (String, Unique) - Firebase authentication ID
- `email` (String) - User email address
- `display_name` (String) - User's display name
- `created_at` (Timestamp) - Account creation timestamp
- `updated_at` (Timestamp) - Last update timestamp

**Itinerary**
- `id` (UUID, PK) - Unique itinerary identifier
- `user_id` (UUID, FK → User) - Creator of the itinerary
- `title` (String) - Itinerary title
- `description` (Text) - Detailed description
- `start_date` (Date) - Trip start date
- `end_date` (Date) - Trip end date
- `is_public` (Boolean) - Visibility flag
- `created_at` (Timestamp) - Creation timestamp
- `updated_at` (Timestamp) - Last update timestamp

**Location**
- `id` (UUID, PK) - Unique location identifier
- `itinerary_id` (UUID, FK → Itinerary) - Associated itinerary
- `name` (String) - Location name
- `country` (String) - Country name
- `latitude` (Decimal) - Geographic latitude
- `longitude` (Decimal) - Geographic longitude
- `arrival_date` (Date) - Arrival date
- `departure_date` (Date) - Departure date
- `order_index` (Integer) - Order in itinerary

**Accommodation**
- `id` (UUID, PK) - Unique accommodation identifier
- `location_id` (UUID, FK → Location) - Associated location
- `name` (String) - Accommodation name
- `address` (String) - Physical address
- `check_in` (DateTime) - Check-in time
- `check_out` (DateTime) - Check-out time
- `notes` (Text) - Additional notes

**Transport**
- `id` (UUID, PK) - Unique transport identifier
- `from_location_id` (UUID, FK → Location) - Origin location
- `to_location_id` (UUID, FK → Location) - Destination location
- `transport_type` (Enum) - Type: FLIGHT, TRAIN, CAR, BUS, BOAT, OTHER
- `departure_time` (DateTime) - Departure time
- `arrival_time` (DateTime) - Arrival time
- `booking_reference` (String) - Booking confirmation number
- `notes` (Text) - Additional details

**Relationships:**
- User → Itinerary (1:N) - A user can create multiple itineraries
- Itinerary → Location (1:N) - An itinerary contains multiple locations
- Location → Accommodation (1:N) - A location can have multiple accommodations
- Location → Transport (1:N) - Transport connects two locations

**Database Configuration:**
- **Production:** Google Cloud SQL PostgreSQL (Sandbox tier / db-f1-micro)
- **Connection:** Private IP via VPC peering
- **High Availability:** Single zone (cost optimization)
- **Backups:** Automated daily backups enabled

---

#### Comments & Likes Service - Firestore

[//]: # (# TODO: Add JSON Schema or document structure diagram for Firestore collections)

**Collections:**

**comments** (Collection)
```json
{
  "id": "string (auto-generated)",
  "itineraryId": "string (UUID)",
  "userId": "string (Firebase UID)",
  "userName": "string",
  "content": "string",
  "createdAt": "timestamp",
  "updatedAt": "timestamp",
  "parentCommentId": "string (optional, for threaded comments)"
}
```

**likes** (Collection)
```json
{
  "id": "string (auto-generated)",
  "itineraryId": "string (UUID)",
  "userId": "string (Firebase UID)",
  "createdAt": "timestamp"
}
```

**Indexes:**
- `comments`: Composite index on `(itineraryId, createdAt DESC)`
- `likes`: Composite index on `(itineraryId, userId)`
- `likes`: Single field index on `itineraryId` for count queries

**Why Firestore?**
- Schema flexibility for evolving social features
- Real-time updates for live comment feeds (future feature)
- Automatic scaling without infrastructure management
- Strong consistency within document operations
- Native Firebase Authentication integration

---

#### Recommendation Service - Neo4j Graph Database

[//]: # (# TODO: Add Graph Data Model diagram showing nodes and relationships)

**Node Types:**

**User Node**
```
Properties:
- userId: string (Firebase UID)
- createdAt: datetime
```

**Itinerary Node**
```
Properties:
- itineraryId: string (UUID)
- title: string
- createdAt: datetime
- likesCount: integer
```

**Location Node**
```
Properties:
- locationId: string (UUID)
- name: string
- country: string
- latitude: float
- longitude: float
```

**Relationship Types:**

**LIKES**
```
(User)-[:LIKES {createdAt: datetime}]->(Itinerary)
```
- Created when a user likes an itinerary
- Weighted in recommendation algorithm

**CREATED**
```
(User)-[:CREATED {createdAt: datetime}]->(Itinerary)
```
- Links itinerary to its creator
- Used for authorship filtering

**VISITED**
```
(User)-[:VISITED {visitDate: date}]->(Location)
```
- Inferred from itinerary locations
- Used for location-based recommendations

**INCLUDES**
```
(Itinerary)-[:INCLUDES {orderIndex: integer}]->(Location)
```
- Represents locations in an itinerary
- Order preserved via orderIndex property

**Graph Queries:**

The recommendation engine uses Cypher queries for collaborative filtering:

```cypher
// Find similar users who liked the same content
MATCH (user:User {userId: $userId})-[:LIKES]->(itinerary:Itinerary)<-[:LIKES]-(similarUser:User)
MATCH (similarUser)-[:LIKES]->(recommendation:Itinerary)
WHERE NOT (user)-[:LIKES]->(recommendation)
RETURN recommendation
ORDER BY recommendation.likesCount DESC
```

**Why Neo4j?**
- Native graph traversal for collaborative filtering
- Efficient "friends-of-friends" queries
- Pattern matching for complex recommendation logic
- Relationship-first data model fits social recommendations

**Database Configuration:**
- **Deployment:** Neo4j container in GKE cluster
- **Persistence:** Kubernetes Persistent Volume
- **Access:** Internal cluster service (not publicly exposed)

---

#### Weather Forecast Service - PostgreSQL Database

[//]: # (# TODO: Add ER Diagram for Weather Forecast Service database schema)

**Entities:**

**WeatherForecast**
- `id` (Long, PK, Auto-increment) - Unique forecast identifier
- `location_name` (String) - Location name
- `latitude` (Decimal) - Geographic latitude
- `longitude` (Decimal) - Geographic longitude
- `fetched_at` (Timestamp) - When data was retrieved
- `timezone` (String) - Location timezone

**DailyForecast**
- `id` (Long, PK, Auto-increment) - Unique daily forecast ID
- `weather_forecast_id` (Long, FK → WeatherForecast) - Parent forecast
- `date` (Date) - Forecast date
- `temperature_high` (Decimal) - High temperature (°C)
- `temperature_low` (Decimal) - Low temperature (°C)
- `weather_condition` (String) - Weather description
- `weather_icon` (String) - Icon identifier
- `precipitation_probability` (Integer) - Rain probability (%)
- `precipitation_total` (Decimal) - Total precipitation (mm)
- `wind_speed` (Decimal) - Wind speed (km/h)
- `wind_direction` (String) - Wind direction
- `humidity` (Integer) - Humidity percentage
- `uv_index` (Integer) - UV index
- `summary` (Text) - Daily summary

**HourlyForecast**
- `id` (Long, PK, Auto-increment) - Unique hourly forecast ID
- `weather_forecast_id` (Long, FK → WeatherForecast) - Parent forecast
- `datetime` (Timestamp) - Forecast datetime
- `temperature` (Decimal) - Temperature (°C)
- `weather_condition` (String) - Weather description
- `weather_icon` (String) - Icon identifier
- `wind_speed` (Decimal) - Wind speed (km/h)
- `wind_direction` (String) - Wind direction
- `precipitation_amount` (Decimal) - Precipitation (mm)
- `precipitation_type` (String) - Rain, snow, etc.
- `cloud_cover` (Integer) - Cloud coverage (%)
- `summary` (Text) - Hourly summary

**Relationships:**
- WeatherForecast → DailyForecast (1:N)
- WeatherForecast → HourlyForecast (1:N)

**Database Configuration:**
- **Deployment:** PostgreSQL container in GKE cluster
- **Persistence:** Kubernetes Persistent Volume
- **Data Retention:** Forecasts updated daily, old data pruned

---

#### Travel Warnings Service - PostgreSQL Database

[//]: # (# TODO: Add ER Diagram for Travel Warnings Service database schema)

**Entities:**

**TravelWarning**
- `id` (Long, PK, Auto-increment) - Unique warning identifier
- `country_code` (String) - ISO country code
- `country_name` (String) - Country name
- `severity` (Enum) - NONE, MINOR, MODERATE, SEVERE, CRITICAL
- `summary` (Text) - Warning summary
- `content_html` (Text) - Full HTML content from API
- `last_updated` (Timestamp) - Last update from source
- `fetched_at` (Timestamp) - When we fetched the data
- `url` (String) - Link to official source

**UserTrip**
- `id` (Long, PK, Auto-increment) - Unique trip identifier
- `user_id` (String) - Firebase UID
- `itinerary_id` (UUID) - Reference to itinerary
- `country_codes` (Array/JSON) - List of countries in trip
- `start_date` (Date) - Trip start date
- `end_date` (Date) - Trip end date
- `notifications_enabled` (Boolean) - Alert preference
- `created_at` (Timestamp) - Registration timestamp

**WarningNotification**
- `id` (Long, PK, Auto-increment) - Unique notification identifier
- `user_trip_id` (Long, FK → UserTrip) - Associated trip
- `travel_warning_id` (Long, FK → TravelWarning) - Associated warning
- `sent_at` (Timestamp) - When notification was sent
- `notification_type` (Enum) - EMAIL, PUSH (future)
- `status` (Enum) - SENT, FAILED, PENDING

**Relationships:**
- UserTrip → WarningNotification (1:N)
- TravelWarning → WarningNotification (1:N)

**Database Configuration:**
- **Deployment:** PostgreSQL container in GKE cluster
- **Persistence:** Kubernetes Persistent Volume
- **Scheduled Jobs:** Quarkus Scheduler checks API every 15 minutes

---

## 3 Runtime View

### 3.1 Runtime Overview

[//]: # (# TODO: Add Cloud Resources Diagram showing GKE cluster, Cloud SQL, Firestore, Load Balancer, etc.)

#### Cloud Infrastructure

**Google Cloud Platform (GCP) - Project:** `<project-id>`

**Core Services:**
- **Google Kubernetes Engine (GKE)** - Primary container orchestration platform
  - Cluster name: `tripico-cluster` (example)
  - Region: `europe-west1` (Belgium)
  
- **Google Cloud SQL** - Managed PostgreSQL (Itinerary Service only)
  - Instance tier: `db-f1-micro` (Sandbox mode - 0.6 GB RAM, shared CPU)
  - Version: PostgreSQL 15
  - Availability: Single zone (cost optimization)
  
- **Google Firestore** - NoSQL document database (Comments & Likes Service)
  - Mode: Native mode
  - Location: Multi-region (automatic)
  - Billing: Pay-per-use
  
- **Google Cloud Storage** - Object storage
  - Bucket for user-uploaded media
  
- **Google Artifact Registry** - Container image registry
  - Repository: `<region>-docker.pkg.dev/<project-id>/tripico-images`
  - Stores Docker images for all microservices
 
- **Firebase Authentication** - Identity platform
  - Authentication methods: Email/Password, Google OAuth
  - Token-based authentication for API requests

#### Network Architecture

- **VPC Network** - Private network for GKE cluster
- **Cloud NAT** - Outbound internet access for pods
- **Internal Services** - Neo4j, PostgreSQL containers accessible only within cluster
- **External Access** - Load balancer → Ingress → Services → Pods

#### Service Communication

**Synchronous Services:**
- All microservices expose REST APIs (synchronous HTTP/HTTPS)
- Frontend communicates with backend via REST APIs
- Service-to-service communication minimal (mostly independent)
- Recommendation Service syncs data via REST calls to Itinerary and Comments services

**Asynchronous Services:**
- Travel Warnings Service uses scheduled jobs (Quarkus Scheduler) for polling
- Weather Service uses scheduled jobs for daily forecast updates
- Email notifications sent asynchronously via Quarkus Mailer

**External API Integration:**
- REST clients with retry logic and caching
- Rate limiting considerations for external APIs

#### Running Application

- **Frontend:** https://frontend.tripico.fun
- **Backend APIs:** Accessible via Kubernetes Ingress (internal routing)
- **Swagger UI:** Available on each service (e.g., `/q/swagger-ui`)
- **Health Checks:** `/q/health` endpoints on all services

#### Configuration Management

- **Kubernetes ConfigMaps** - Application configuration (non-sensitive)
- **Kubernetes Secrets** - Database passwords, API keys, Firebase credentials
- **Environment Variables** - Runtime configuration injection
- **Quarkus Profiles** - Environment-specific configs (dev, prod)

### 3.2 Microservices

#### 3.2.1 Itinerary Service

**Description:**  
The Itinerary Service is the core backend service managing user accounts and travel itineraries. It provides CRUD operations for users, itineraries, locations, accommodations, and transport details.

**Software Components:**
- Java 21 / Quarkus 3.28.1
- Hibernate ORM with Panache (PostgreSQL)
- Firebase Admin SDK (authentication)
- MapStruct (DTO mapping)
- SmallRye OpenAPI (API documentation)

**Runtime Configuration:**
- **Container Image:** Built via Dockerfile, pushed to Google Artifact Registry
- **Deployment:** Kubernetes Deployment in GKE
- **Replicas:** Initially 2, auto-scaling enabled
- **Resource Limits:**
  ```yaml
  resources:
    requests:
      memory: "512Mi"
      cpu: "250m"
    limits:
      memory: "1Gi"
      cpu: "500m"
  ```
- **Environment Variables:** Database connection, Firebase config, service URLs
- **Health Checks:** Liveness and readiness probes on `/q/health`

**Scalability:**
- **Auto-scaling:** Horizontal Pod Autoscaler (HPA) configured
  - Target CPU utilization: 70%
  - Min replicas: 2
  - Max replicas: 10
- **Scaling Type:** Automatic based on CPU metrics
- **Database Bottleneck:** Cloud SQL instance is a potential bottleneck (shared db-f1-micro)

**Security:**
- **Authentication:** Firebase JWT token validation on all protected endpoints
- **Authorization:** User can only modify their own resources
- **TLS/SSL:** Enforced via Ingress (HTTPS only)
- **Network Policies:** Kubernetes NetworkPolicies restrict pod-to-pod communication
- **Secrets Management:** Database credentials stored in Kubernetes Secrets

**External Connections:**
- **Google Cloud SQL (PostgreSQL)** - Primary data store via private IP
- **Firebase Authentication** - Token verification
- **Recommendation Service** - Webhook/sync calls when itineraries are created/updated

**API Endpoints:**
- `GET/POST /api/users` - User management
- `GET/POST/PUT/DELETE /api/itineraries` - Itinerary CRUD
- `GET /api/itineraries/search` - Search functionality
- `POST /api/itineraries/{id}/locations` - Add locations
- `GET /q/health` - Health checks

---

#### 3.2.2 Recommendation Service

**Description:**  
The Recommendation Service provides personalized travel recommendations using graph-based collaborative filtering. It maintains a Neo4j graph database modeling relationships between users, itineraries, and locations.

**Software Components:**
- Java 21 / Quarkus 3.x
- Neo4j Java Driver 5.x
- REST Client (for data synchronization)
- Custom graph traversal algorithms

**Runtime Configuration:**
- **Container Image:** Built via Dockerfile, pushed to Google Artifact Registry
- **Deployment:** Kubernetes Deployment + StatefulSet for Neo4j
- **Replicas:** 2-10 application pods, 1 Neo4j pod
- **Resource Limits:**
  ```yaml
  # Application pods
  resources:
    requests:
      memory: "512Mi"
      cpu: "250m"
    limits:
      memory: "1Gi"
      cpu: "1000m"
  # Neo4j pod
  resources:
    requests:
      memory: "1Gi"
      cpu: "500m"
    limits:
      memory: "2Gi"
      cpu: "2000m"
  ```
- **Persistent Storage:** Neo4j data stored on Kubernetes Persistent Volume (PVC)

**Scalability:**
- **Auto-scaling:** HPA enabled for application pods
  - Target CPU utilization: 75%
  - Min replicas: 1
  - Max replicas: 5
- **Scaling Type:** Automatic for stateless recommendation API, manual for Neo4j database
- **Graph Database:** Neo4j is currently single-instance; horizontal scaling requires Neo4j Enterprise (causal clustering)

**Security:**
- **Authentication:** Firebase JWT validation
- **Network Isolation:** Neo4j accessible only within Kubernetes cluster (ClusterIP service)
- **Database Credentials:** Neo4j password stored in Kubernetes Secret
- **TLS/SSL:** HTTPS via Ingress

**External Connections:**
- **Neo4j Database** - Graph database (internal, port 7687)
- **Itinerary Service** - Pulls itinerary data for graph sync
- **Comments & Likes Service** - Pulls like data for social signals

**API Endpoints:**
- `GET /api/recommendations/feed` - Personalized recommendation feed
- `POST /api/recommendations/sync` - Trigger graph data synchronization (internal)
- `GET /q/health` - Health checks

**Graph Synchronization:**
- Periodic sync job pulls data from Itinerary and Comments services
- Real-time updates via webhook (when user likes/creates content)
- Incremental updates to avoid full graph rebuilds

---

#### 3.2.3 Comments & Likes Service

**Description:**  
The Comments & Likes Service handles all social interactions on itineraries, including comments and likes. It uses Firestore for flexible, scalable document storage.

**Software Components:**
- Java 21 / Quarkus 3.x
- Google Cloud Firestore SDK
- Firebase Admin SDK (authentication)
- Quarkus REST

**Runtime Configuration:**
- **Container Image:** Built via Dockerfile, pushed to Google Artifact Registry
- **Deployment:** Kubernetes Deployment in GKE
- **Replicas:** 2-8 pods
- **Resource Limits:**
  ```yaml
  resources:
    requests:
      memory: "256Mi"
      cpu: "200m"
    limits:
      memory: "512Mi"
      cpu: "500m"
  ```
- **No Local Storage Required:** Firestore is fully managed (serverless)

**Scalability:**
- **Auto-scaling:** HPA enabled
  - Target CPU utilization: 70%
  - Min replicas: 2
  - Max replicas: 10
- **Scaling Type:** Automatic horizontal scaling
- **Database Scalability:** Firestore scales automatically without limits

**Security:**
- **Authentication:** Firebase JWT validation
- **Authorization:** Users can only edit/delete their own comments
- **Firestore Security Rules:** Server-side validation (Admin SDK bypasses client rules)
- **TLS/SSL:** HTTPS enforced

**External Connections:**
- **Google Firestore** - Primary data store (managed service)
- **Firebase Authentication** - Token verification
- **Recommendation Service** - Notifies when likes are added (for graph sync)

**API Endpoints:**
- `GET/POST /api/comments` - Comment CRUD
- `GET/POST/DELETE /api/likes` - Like operations
- `GET /api/itineraries/{id}/likes/count` - Get likes count
- `GET /q/health` - Health checks

---

#### 3.2.4 Travel Warnings Service

**Description:**  
The Travel Warnings Service fetches real-time travel advisories from the Auswärtiges Amt API, matches them with user trips, and sends email notifications when warnings affect registered trips.

**Software Components:**
- Java 21 / Quarkus 3.29.3
- Hibernate ORM with Panache (PostgreSQL)
- Quarkus REST Client (Auswärtiges Amt API)
- Quarkus Mailer (email notifications)
- Quarkus Scheduler (periodic polling)
- Caffeine Cache (API response caching)

**Runtime Configuration:**
- **Container Image:** Built via Dockerfile, pushed to Google Artifact Registry
- **Deployment:** Kubernetes Deployment in GKE
- **Replicas:** 2-10 pods (only one actively polls API, others on standby)
- **Resource Limits:**
  ```yaml
  resources:
    requests:
      memory: "512Mi"
      cpu: "250m"
    limits:
      memory: "1Gi"
      cpu: "500m"
  ```
- **Persistent Storage:** PostgreSQL container with PVC (5Gi)
- **Scheduled Jobs:** Quarkus Scheduler runs every 15 minutes

**Scalability:**
- **Auto-scaling:** HPA configured but limited effectiveness (scheduled job runs on all pods)
  - Target CPU utilization: 75%
  - Min replicas: 2
  - Max replicas: 10
- **Scaling Type:** Manual scaling recommended for this workload
- **Optimization:** Leader election could enable better horizontal scaling

**Security:**
- **Authentication:** Firebase JWT for API endpoints
- **Email Security:** SMTP credentials stored in Kubernetes Secret (SendGrid)
- **API Rate Limiting:** Caching reduces external API calls
- **TLS/SSL:** HTTPS enforced

**External Connections:**
- **PostgreSQL Database** - Internal container in cluster (port 5432)
- **Auswärtiges Amt API** - External REST API (HTTPS)
- **Email Service (SendGrid)** - SMTP for notifications
- **Itinerary Service** - Fetches user trip data for matching

**API Endpoints:**
- `GET /api/warnings` - Retrieve travel warnings
- `GET /api/warnings/country/{code}` - Warnings for specific country
- `POST /api/trips` - Register user trip for alerts
- `PUT /api/trips/{id}/preferences` - Update notification settings
- `GET /q/health` - Health checks

**Notification Logic:**
1. Scheduled job fetches latest warnings from Auswärtiges Amt (every 15 min)
2. Compares with previous state to detect changes
3. Queries UserTrips to find affected trips (matching countries and dates)
4. Sends HTML email alerts to users with enabled notifications
5. Records notification in WarningNotification table

---

#### 3.2.5 Weather Forecast Service

**Description:**  
The Weather Forecast Service integrates with the Meteosource API to provide 7-day daily and 24-hour hourly weather forecasts for travel destinations.

**Software Components:**
- Java 21 / Quarkus 3.x
- Hibernate ORM with Panache (PostgreSQL)
- Quarkus REST Client (Meteosource API)
- Quarkus Scheduler (daily updates)

**Runtime Configuration:**
- **Container Image:** Built via Dockerfile, pushed to Google Artifact Registry
- **Deployment:** Kubernetes Deployment in GKE
- **Replicas:** 2-10 pods
- **Resource Limits:**
  ```yaml
  resources:
    requests:
      memory: "512Mi"
      cpu: "250m"
    limits:
      memory: "1Gi"
      cpu: "500m"
  ```
- **Persistent Storage:** PostgreSQL container with PVC (5Gi)
- **Environment Variables:** Meteosource API key (Kubernetes Secret)

**Scalability:**
- **Auto-scaling:** HPA enabled
  - Target CPU utilization: 70%
  - Min replicas: 2
  - Max replicas: 10
- **Scaling Type:** Automatic horizontal scaling
- **Read-Heavy Workload:** Caching could improve performance under high load

**Security:**
- **Authentication:** Firebase JWT validation
- **API Key Security:** Meteosource key stored in Kubernetes Secret
- **TLS/SSL:** HTTPS enforced
- **Rate Limiting:** Meteosource free tier limits apply

**External Connections:**
- **PostgreSQL Database** - Internal container in cluster (port 5432)
- **Meteosource API** - External REST API for weather data

**API Endpoints:**
- `GET /api/weather/daily` - 7-day daily forecast
- `GET /api/weather/hourly` - 24-hour hourly forecast
- `GET /api/weather/location` - Weather for specific coordinates
- `GET /q/health` - Health checks

**Data Update Strategy:**
- Daily scheduled job updates forecasts for popular destinations
- On-demand fetch for user-requested locations not in cache
- Old forecasts pruned to manage database size

---

### 3.3 Datastores

**Summary of Runtime Storage Containers:**

| Datastore | Technology | Location | Service(s) Using It | Data Model Link |
|-----------|-----------|----------|---------------------|-----------------|
| **Cloud SQL Instance** | PostgreSQL 15 | GCP Managed Service | Itinerary Service | [See Section 2.2 - Itinerary Service DB](#itinerary-service---postgresql-database) |
| **Firestore Database** | Google Firestore | GCP Managed Service | Comments & Likes Service | [See Section 2.2 - Firestore](#comments--likes-service---firestore) |
| **Neo4j Database** | Neo4j 5.x | GKE StatefulSet + PVC | Recommendation Service | [See Section 2.2 - Neo4j Graph DB](#recommendation-service---neo4j-graph-database) |
| **PostgreSQL Container** | PostgreSQL 15 | GKE Deployment + PVC | Weather Forecast Service | [See Section 2.2 - Weather DB](#weather-forecast-service---postgresql-database) |
| **PostgreSQL Container** | PostgreSQL 15 | GKE Deployment + PVC | Travel Warnings Service | [See Section 2.2 - Travel Warnings DB](#travel-warnings-service---postgresql-database) |

**Storage Volumes:**
- **Kubernetes Persistent Volumes (PVC)** - Used for Neo4j and containerized PostgreSQL databases
- **Storage Class:** Standard persistent disk (pd-standard)
- **Backup Strategy:** 
  - Cloud SQL: Automated daily backups (7-day retention)
  - Containerized databases: Manual backup via CronJobs (future implementation)

---

## 4 DevOps

### 4.1 IaC (Infrastructure as Code)

#### Terraform Setup

**Repository:** https://github.com/leotschritter/cad-backend

**IaC Directory Structure:**
```
services/itinerary-service/terraform/
├── main.tf                    # Root module orchestration
├── provider.tf                # GCP provider configuration
├── variables.tf               # Input variables
├── outputs.tf                 # Output values
└── modules/
    ├── project/               # GCP project & API enablement
    ├── iam/                   # Service accounts & IAM roles
    ├── database/              # Cloud SQL instance
    ├── storage/               # Cloud Storage buckets
    ├── firestore/             # Firestore database setup
    ├── artifact-registry/     # Container registry
    └── gke/                   # GKE cluster configuration
```

#### Managed Resources

**1. GCP Project Configuration (modules/project)**
- Enables required GCP APIs:
  - Compute Engine API
  - Kubernetes Engine API
  - Cloud SQL Admin API
  - Firestore API
  - Cloud Storage API
  - Artifact Registry API
  - Cloud Resource Manager API
  - IAM API
  - Secret Manager API
- Configures Firebase Identity Platform
- Sets authorized domains for authentication

**2. IAM & Service Accounts (modules/iam)**
- Creates service account for application workloads
- Assigns IAM roles:
  - `roles/cloudsql.client` - Cloud SQL access
  - `roles/datastore.user` - Firestore access
  - `roles/storage.objectViewer` - Cloud Storage read access
  - `roles/secretmanager.secretAccessor` - Secret access
- Workload Identity binding for GKE pods

**3. Cloud SQL Database (modules/database)**
- PostgreSQL 15 instance configuration
- Instance tier: `db-f1-micro` (0.6 GB RAM, shared CPU)
- Availability: Single zone (ZONAL)
- Disk size: 10 GB SSD (auto-expandable)
- Private IP configuration (VPC peering)
- Automated backups enabled (7-day retention)
- Database creation: `itinerary_db`
- User creation with secure password (stored in Secret Manager)
- Deletion protection configurable via variable

**4. Google Cloud Storage (modules/storage)**
- Bucket creation for file uploads (future feature)
- Uniform bucket-level access
- Versioning disabled (cost optimization)
- Lifecycle policies (future implementation)
- Service account access binding

**5. Firestore Database (modules/firestore)**
- Firestore Native mode initialization
- Location: Multi-region (automated by GCP)
- Indexes created for queries:
  - Comments: `(itineraryId, createdAt DESC)`
  - Likes: `(itineraryId, userId)`

**6. Artifact Registry (modules/artifact-registry)**
- Docker repository creation
- Repository name: `tripico-images`
- Location: Same region as GKE cluster
- IAM bindings for CI/CD pipelines

**7. GKE Cluster (modules/gke)**
- VPC network and subnet creation
- GKE cluster configuration:
  - Release channel: REGULAR
  - Node pool: Auto-scaling (1-5 nodes)
  - Machine type: e2-medium (2 vCPU, 4 GB RAM)
  - Disk size: 50 GB per node
  - Workload Identity enabled
  - HTTP load balancing enabled
  - Network policy enabled
- Secondary IP ranges for pods and services
- Cluster autoscaler configuration

#### Terraform Workflow

**1. Local Development:**
```bash
cd services/itinerary-service/terraform/
terraform init
terraform plan -var-file="terraform.tfvars"
terraform apply -var-file="terraform.tfvars"
```

**2. Variable Configuration:**
- `terraform.tfvars` file contains environment-specific values
- Sensitive values (passwords, API keys) passed via environment variables
- Example variables:
  - `project_id` - GCP project ID
  - `region` - Deployment region
  - `db_password` - Database password (from env var)
  - `deletion_protection` - Enable/disable resource protection

**3. State Management:**
- Terraform state stored in GCS bucket (remote backend)
- State locking enabled to prevent concurrent modifications
- Backend configuration:
  ```hcl
  terraform {
    backend "gcs" {
      bucket = "tripico-terraform-state"
      prefix = "terraform/state"
    }
  }
  ```

**4. CI/CD Integration:**
- Terraform runs in GitHub Actions pipeline

#### Kubernetes Configuration

**Helm Charts:**
- Weather Forecast Service: `services/weather-forecast-service/helm/`
- Comments & Likes Service: `services/comments-likes-service/comments-likes-chart/`
- Travel Warnings Service: `services/travel_warnings/travel-warnings-chart/`
- Recommendation Service: Helm chart in development

**Chart Structure:**
```
helm-chart/
├── Chart.yaml              # Chart metadata
├── values.yaml             # Default values (dev environment)
├── values-prod.yaml        # Production overrides
└── templates/
    ├── deployment.yaml     # Deployment manifest
    ├── service.yaml        # Service manifest
    ├── ingress.yaml        # Ingress rules
    ├── hpa.yaml            # Horizontal Pod Autoscaler
    ├── configmap.yaml      # Application configuration
    ├── secret.yaml         # Sensitive data (sealed secrets)
    └── certificate.yaml    # SSL certificate (cert-manager)
```

**Helm Deployment:**
```bash
# Install/upgrade chart
helm upgrade --install <release-name> ./helm-chart \
  -f values-prod.yaml \
  --namespace tripico \
  --create-namespace

# Example for weather service
helm upgrade --install weather-forecast ./helm/weather-forecast-service \
  -f helm/weather-forecast-service/values-prod.yaml \
  --namespace tripico
```

#### Docker Build & Push

**Dockerfile Locations:**
- `services/itinerary-service/Dockerfile`
- `services/recommendation-service/Dockerfile`
- `services/comments-likes-service/Dockerfile`
- `services/travel_warnings/Dockerfile`
- `services/weather-forecast-service/Dockerfile`

**Build Process:**
```bash
# Build Docker image
docker build -t <region>-docker.pkg.dev/<project-id>/tripico-images/<service>:<tag> .

# Push to Artifact Registry
docker push <region>-docker.pkg.dev/<project-id>/tripico-images/<service>:<tag>
```

**Multi-stage Builds:**
All services use multi-stage Dockerfiles for optimized image size:
1. Build stage: Maven build with full JDK
2. Runtime stage: Distroless Java runtime with only the JAR file

#### Infrastructure Deployment Summary

1. **Terraform** provisions GCP infrastructure (network, databases, GKE cluster)
2. **Docker** builds containerized microservices
3. **Artifact Registry** stores Docker images
4. **Helm** deploys applications to GKE with environment-specific configurations
5. **Kubernetes** manages container lifecycle, scaling, and networking

---

## 5 Performance Tests

### Overview

A comprehensive load testing suite has been developed using **Locust** (Python-based load testing framework) to evaluate Tripico's performance under different traffic scenarios. The tests simulate realistic user behavior across all five microservices.

**Load Testing Repository:** https://github.com/leotschritter/cad-backend/tree/main/load

**Key Features:**
- Multi-service testing (all 5 microservices)
- Coordinated operations (e.g., like updates both Comments service AND Recommendation graph)
- Realistic transaction mixes
- Automated test execution scripts
- Firebase authentication integration

---

### 5.1 Periodic Workload

#### Test Objective
Simulate normal daily traffic patterns with users browsing, creating content, and interacting throughout the day. Two scenarios test the application under moderate and high periodic load.

---

#### Scenario 1: Moderate Periodic Load (100 Peak Users)

[//]: # (# TODO: Add response time charts &#40;min, median, 95th percentile, max&#41;)
[//]: # (# TODO: Add failure rate chart over time)
[//]: # (# TODO: Add resource utilization graphs &#40;CPU, memory, network&#41;)

**Test Configuration:**

| Parameter | Value |
|-----------|-------|
| **Peak concurrent users** | 100 |
| **Low demand users** | 10 |
| **Test duration** | 20 minutes |
| **Ramp-up time** | 2 minutes (5 users/second) |
| **Ramp-down time** | 1 minute |
| **Peak duration** | 12 minutes |
| **Total users spawned** | ~1000-2000 (with user churn) |

**Initial Data:**
- 50 users seeded via `seed_data.py`
- 200 itineraries (mix of public/private)
- 100 locations across itineraries
- 300 likes distributed across itineraries
- 150 comments on popular itineraries
- Graph database synced with initial data

**Transaction Mix:**

| Operation Type | Weight | Example Actions |
|---------------|--------|-----------------|
| **Browse/Search** | 45% | Search itineraries, view details, list locations |
| **Social Interactions** | 25% | Like/unlike itineraries, read comments, post comments |
| **Recommendations** | 15% | View personalized feed, discover content |
| **Content Creation** | 10% | Create itineraries, add locations, update trips |
| **User Operations** | 5% | Profile updates, login, authentication |

**Expected Results:**

[//]: # (# TODO: Insert actual test results table with the following metrics:)
[//]: # (# - Endpoint name)
[//]: # (# - Request count)
[//]: # (# - Failure count)
[//]: # (# - Median response time &#40;ms&#41;)
[//]: # (# - 95th percentile &#40;ms&#41;)
[//]: # (# - Average requests per second &#40;RPS&#41;)

**Analysis:**

[//]: # (# TODO: Write analysis of results:)
[//]: # (# - Did the application handle the load without degradation?)
[//]: # (# - What was the failure rate? &#40;<1% is acceptable&#41;)
[//]: # (# - Were response times within SLA? &#40;<500ms for reads, <1s for writes&#41;)
[//]: # (# - Did auto-scaling trigger? How many pods were running at peak?)
[//]: # (# - Which service/endpoint was the slowest?)
[//]: # (# - Any bottlenecks identified?)

---

#### Scenario 2: High Periodic Load (1000 Peak Users)

[//]: # (# TODO: Add response time charts &#40;min, median, 95th percentile, max&#41;)
[//]: # (# TODO: Add failure rate chart over time)
[//]: # (# TODO: Add resource utilization graphs &#40;CPU, memory, network&#41;)

**Test Configuration:**

| Parameter | Value |
|-----------|-------|
| **Peak concurrent users** | 1000 |
| **Low demand users** | 20 |
| **Test duration** | 30 minutes |
| **Ramp-up time** | 5 minutes (20 users/second) |
| **Ramp-down time** | 2 minutes |
| **Peak duration** | 20 minutes |
| **Total users spawned** | ~10,000-20,000 (with user churn) |

**Initial Data:**
- 100 users seeded
- 500 itineraries
- 300 locations
- 1000 likes
- 500 comments
- Graph database synced

**Transaction Mix:**
Same as Scenario 1 (45% browse, 25% social, 15% recommendations, 10% creation, 5% user ops)

**Expected Results:**

[//]: # (# TODO: Insert actual test results table)

**Analysis:**

[//]: # (# TODO: Write analysis comparing high load vs moderate load:)
[//]: # (# - How did response times degrade compared to 100-user test?)
[//]: # (# - At what point did auto-scaling kick in?)
[//]: # (# - What was the max number of pods running?)
[//]: # (# - Did any services reach their resource limits?)
[//]: # (# - Were there cascading failures?)
[//]: # (# - Which database became the bottleneck &#40;Cloud SQL, Firestore, Neo4j&#41;?)
[//]: # (# - Recommendations for handling 1000+ concurrent users)

---

### 5.2 Once-in-a-Lifetime Workload

#### Test Objective
Simulate a viral traffic spike scenario where Tripico gains sudden massive popularity (e.g., featured in media, social media viral post). The test continuously adds new users to determine the system's breaking point.

---

#### Test Configuration

[//]: # (# TODO: Add user growth chart showing exponential increase)
[//]: # (# TODO: Add response time degradation chart)
[//]: # (# TODO: Add failure rate chart showing point of system failure)

| Parameter | Value |
|-----------|-------|
| **Starting users** | 10 |
| **User growth rate** | 10 new users every 10 seconds |
| **Max target users** | 2000+ (or until system fails) |
| **Test duration** | 30 minutes |
| **Ramp-up strategy** | Linear, continuous growth |
| **Total users spawned** | 5000+ |

**Initial Data:**
- 50 users seeded
- 200 itineraries
- 5-10 "viral" itineraries (heavily accessed)
- 100 locations
- 500 likes concentrated on viral content
- 200 comments on viral itineraries

**Transaction Mix (Viral Pattern):**

| Operation Type | Weight | Target | Notes |
|---------------|--------|--------|-------|
| **Hot Spot Reads** | 65% | 5-10 viral itineraries | 90% of reads target same items |
| **Burst Social Actions** | 20% | Viral content | Likes and comments on trending trips |
| **Discovery/Search** | 10% | Popular searches | Users searching for trending locations |
| **Content Creation** | 5% | New itineraries | Inspired users creating copycat trips |


---

#### Results & Analysis

[//]: # (# TODO: Insert test results with three phases:)

**Phase 1: No Degradation**

[//]: # (# TODO: Document user count range where system performs normally)
[//]: # (# - User count: 10 - ??? users)
[//]: # (# - Response times: <500ms &#40;95th percentile&#41;)
[//]: # (# - Failure rate: <1%)
[//]: # (# - Resource utilization: ???)

**Phase 2: Degradation**

[//]: # (# TODO: Document user count range where system slows but survives)
[//]: # (# - User count: ??? - ??? users)
[//]: # (# - Response times: 500ms - 2000ms &#40;95th percentile&#41;)
[//]: # (# - Failure rate: 1-10%)
[//]: # (# - Resource utilization: ???)
[//]: # (# - Observed issues: ???)
**Phase 3: System Failure**

[//]: # (# TODO: Document breaking point)
[//]: # (# - User count: ??? + users)
[//]: # (# - Response times: >2000ms or timeouts)
[//]: # (# - Failure rate: >10%)
[//]: # (# - Failure modes: ???)

**Bottleneck Analysis:**

[//]: # (# TODO: Identify which component failed first:)
[//]: # (# - [ ] Cloud SQL connection pool exhaustion?)
[//]: # (# - [ ] GKE node resource limits &#40;CPU/memory&#41;?)
[//]: # (# - [ ] Network bandwidth saturation?)
[//]: # (# - [ ] Neo4j graph query performance?)
[//]: # (# - [ ] Firestore read/write quotas?)
[//]: # (# - [ ] Application thread pool exhaustion?)
[//]: # (# - [ ] Load balancer connection limits?)

**Key Findings:**

[//]: # (# TODO: Summarize key findings:)
[//]: # (# - Maximum sustainable concurrent users: ???)
[//]: # (# - Estimated requests per second at failure: ???)
[//]: # (# - Primary bottleneck: ???)
[//]: # (# - Secondary bottlenecks: ???)
[//]: # (# - Cost implications of handling viral load: ???)

**Recommendations:**

[//]: # (# TODO: Provide recommendations:)
[//]: # (# - Increase GKE node pool max size to ???)
[//]: # (# - Upgrade Cloud SQL instance to db-??? tier)
[//]: # (# - Implement caching layer &#40;Redis&#41; for hot content)
[//]: # (# - Add rate limiting to protect against abuse)
[//]: # (# - Implement request queuing for burst traffic)
[//]: # (# - Consider CDN for static content)
[//]: # (# - Enable read replicas for Cloud SQL)
[//]: # (# - Implement circuit breakers between services)
[//]: # (# - Add monitoring alerts for resource saturation)

---

## Appendix

[//]: # (# TODO: Add appendices as needed:)
[//]: # (# - A: Full API documentation links)
[//]: # (# - B: Deployment runbooks)
[//]: # (# - C: Disaster recovery procedures)
[//]: # (# - D: Cost analysis)
[//]: # (# - E: Future enhancements roadmap)

---

**End of Report**

