# Cloud Native Project - Cloud Application Development – Winter Term 2025/26
## Team Bermuda Dreieck

**Team Members:** Leo Tschritter, Dennis Hoang, Benedikt Scheffel

**Date:** January 27, 2026

---

## Table of Contents
1. [Requirements](#1-requirements)
   - 1.1 [System Context](#11-system-context)
   - 1.2 [Feature Overview](#12-feature-overview)
   - 1.3 [Domain Model](#13-domain-model)
2. [Runtime View](#2-runtime-view)
   - 2.1 [Runtime Overview](#21-runtime-overview)
   - 2.2 [Microservices](#22-microservices)
   - 2.3 [Datastores](#23-datastores)
   - 2.4 [Security: Roles and Role Mapping](#24-security-roles-and-role-mapping)
3. [Development View](#3-development-view)
   - 3.1 [Software Components](#31-software-components)
4. [DevOps](#4-devops)
   - 4.1 [Environments and Initial Infrastructure Setup](#41-environments-and-initial-infrastructure-setup)
   - 4.2 [Pipelines and Release of new Features](#42-pipelines-and-release-of-new-features)
   - 4.3 [Creation of new Tenants](#43-creation-of-new-tenants)
   - 4.4 [Monitoring](#44-monitoring)
5. [Performance Tests](#5-performance-tests)
   - 5.1 [Periodic Workload](#51-periodic-workload)
   - 5.2 [Once-in-a-lifetime Workload](#52-once-in-a-lifetime-workload)
6. [Commercial Model](#6-commercial-model)
   - 6.1 [Tenant Types](#61-tenant-types)
   - 6.2 [Pricing Model](#62-pricing-model)
   - 6.3 [Cost Model](#63-cost-model)

---

## 1 Requirements

### Introduction

**Tripico** is a cloud-native, multi-tenant travel planning application that enables users to create, share, and discover travel itineraries. The system provides comprehensive travel planning features including itinerary management, personalized recommendations, real-time weather forecasts, travel warnings, and social interactions through comments and likes.

The application is built as a microservices architecture deployed on Google Cloud Platform (GCP), utilizing GKE Autopilot for orchestration, Firestore for document storage, Neo4j for graph-based recommendations, and Google Identity Platform for multi-tenant user authentication.

**Multi-Tenancy Model:**

Tripico implements a three-tier multi-tenancy architecture to serve different customer segments:

| Tier | Model | Target Customer |
|------|-------|-----------------|
| **Freemium** | Shared cluster, shared namespace, no tenant ID | Individual users, trial accounts |
| **Standard** | Shared cluster, dedicated namespace, dedicated tenant ID | Small-medium businesses |
| **Enterprise** | Dedicated cluster, full isolation, dedicated tenant ID | Large organizations |

**Environments:**
- **Production Environment:** Multi-tenant deployment accessible at https://frontend-freemium.tripico.fun
- **Development Environment:** Mirror environment for testing at https://dev.frontend-freemium.tripico.fun

### 1.1 System Context

The Tripico system consists of six core microservices that work together to provide travel planning functionality:

![system_context-Tripico_System_Context.png](img/system_context-Tripico_System_Context.png)

#### Microservices Overview

| Service | Responsibility | Key Endpoints |
|---------|---------------|---------------|
| **Itinerary Service** | Core service managing users, itineraries, locations, accommodations, and transport | `/api/users`, `/api/itineraries`, `/api/locations` |
| **Comments & Likes Service** | Social interactions on itineraries | `/api/comments`, `/api/likes` |
| **Recommendation Service** | Personalized recommendations using graph-based collaborative filtering | `/api/feed`, `/api/graph` |
| **Weather Forecast Service** | Weather data for travel destinations | `/api/weather/daily`, `/api/weather/hourly` |
| **Travel Warnings Service** | Travel safety information and email alerts | `/api/warnings`, `/api/trips` |
| **Tenant Service** | Multi-tenancy management, tenant configuration and onboarding | `/api/v1/tenants` |

#### Service Communication

- **Itinerary Service** is the core service that manages user and trip data
- **Comments & Likes Service** references itineraries to attach social interactions
- **Recommendation Service** syncs data from Itinerary and Comments services to build the recommendation graph
- **Weather Forecast Service** provides weather data for locations in itineraries
- **Travel Warnings Service** monitors trips and sends alerts when warnings affect destinations
- **Tenant Service** provides tenant configuration to all services and handles tenant onboarding

#### Neighboring Systems

| System | Used By | Purpose |
|--------|---------|---------|
| **Auswärtiges Amt API** | Travel Warnings Service | German Federal Foreign Office API providing real-time travel warnings |
| **Meteosource API** | Weather Forecast Service | External weather data provider for forecasts |
| **Google Identity Platform** | All Services | Multi-tenant authentication and user management |
| **SMTP Server** | Travel Warnings Service | Email delivery for travel alert notifications |

#### Actors

| Actor | Description |
|-------|-------------|
| **Traveler** | End user who creates itineraries, browses recommendations, and interacts with content |

#### User Interfaces

- **Web Frontend** - Vue.js/Vuetify-based single-page application (https://frontend-freemium.tripico.fun)
- **REST APIs** - OpenAPI-documented endpoints for all microservices

### 1.2 Feature Overview

#### Core Features (All Tiers)

**1. Itinerary Management**
- Create and read travel itineraries
- Add locations, accommodations, and transport details to trips
- Search and filter itineraries by various criteria

**2. Personalized Recommendations (Graph-based)**
- Recommendation feed using Neo4j graph database
- Collaborative filtering: "Users who liked what you liked also liked..."
- Location-based recommendations based on visited destinations
- Social signals integration (popularity based on likes count)

**3. Travel Warnings & Alerts**
- Real-time travel warning information from Auswartiges Amt
- Automated notification system for trips affected by new warnings
- Email alerts with severity levels (None, Minor, Moderate, Severe, Critical)

**4. Weather Forecasts**
- 7-day daily weather forecasts for travel destinations
- 24-hour hourly forecasts with detailed metrics
- Temperature, precipitation, wind, humidity, and UV index

**5. Social Interactions**
- Like and unlike itineraries
- Comment on itineraries with threaded discussions
- View likes count and engagement metrics

#### Tier-Specific Features

| Feature | Freemium | Standard | Enterprise |
|---------|----------|----------|------------|
| Itinerary Creation | 10 max | Unlimited | Unlimited |
| Recommendations | Basic | Advanced | Advanced + Custom |
| Auto-scaling | OFF | ON (2 replicas) | ON (10 replicas) |
| User Isolation | None | Tenant-level | Full isolation |
| SLA | None | 99% uptime | 99.9% uptime |
| Support | Community | Email | 24/7 dedicated |

### 1.3 Domain Model

The Tripico domain model consists of the following core business entities:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           TRIPICO DOMAIN MODEL                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────┐     creates      ┌─────────────┐     contains    ┌──────────┐│
│  │   User   │─────────────────>│  Itinerary  │───────────────>│ Location ││
│  │          │                  │             │                 │          ││
│  │ tenantId │     likes        │ isPublic    │     has         │ country  ││
│  │ email    │─────────────────>│ startDate   │<────────────────│ lat/long ││
│  │ name     │                  │ endDate     │                 │ dates    ││
│  └──────────┘                  └─────────────┘                 └──────────┘│
│       │                              │                              │       │
│       │ comments                     │                              │       │
│       v                              │                              v       │
│  ┌──────────┐                        │                        ┌──────────┐ │
│  │ Comment  │<───────────────────────┘                        │Accommoda-│ │
│  │          │                                                 │  tion    │ │
│  │ content  │                                                 │          │ │
│  │ parentId │     ┌──────────────────────────────────────────>│ checkIn  │ │
│  └──────────┘     │                                           │ checkOut │ │
│                   │                                           └──────────┘ │
│       ┌───────────┴───────────┐                                            │
│       │      Like             │          ┌───────────────────────────────┐ │
│       │                       │          │        Transport              │ │
│       │  userId + itineraryId │          │                               │ │
│       │  createdAt            │          │  fromLocation -> toLocation   │ │
│       └───────────────────────┘          │  type: FLIGHT|TRAIN|CAR|...   │ │
│                                          └───────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────────────┤
│                         MULTI-TENANCY CONTEXT                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────┐     belongs to    ┌──────────┐     has         ┌──────────┐  │
│  │   User   │──────────────────>│  Tenant  │────────────────>│ Identity │  │
│  │          │                   │          │                 │ Platform │  │
│  │ tenantId │                   │ tier     │                 │ Tenant   │  │
│  │ (or null │                   │ name     │                 │          │  │
│  │  for     │                   │ config   │                 │ std-xxx  │  │
│  │  freemium)│                  │          │                 │ ent-xxx  │  │
│  └──────────┘                   └──────────┘                 └──────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Key Entities

**User**
- `id` (UUID) - Unique user identifier
- `firebase_uid` (String) - Firebase/Identity Platform authentication ID
- `tenant_id` (String, nullable) - Tenant identifier (null for freemium users)
- `email` (String) - User email address
- `display_name` (String) - User's display name

**Tenant** (Conceptual - stored in Identity Platform)
- `id` (String) - Tenant identifier (e.g., "std-tenant-1", "ent-acme")
- `tier` (Enum) - FREEMIUM, STANDARD, ENTERPRISE
- `display_name` (String) - Human-readable tenant name
- `config` (Object) - Tier-specific configuration

**Itinerary**
- `id` (UUID) - Unique itinerary identifier
- `user_id` (UUID) - Creator reference
- `title`, `description` - Content
- `start_date`, `end_date` - Trip dates
- `is_public` (Boolean) - Visibility flag

**Location**
- `id` (UUID) - Unique location identifier
- `itinerary_id` (UUID) - Parent itinerary
- `name`, `country` - Location details
- `latitude`, `longitude` - Coordinates
- `arrival_date`, `departure_date` - Visit dates

**Relationships:**
- User creates Itinerary (1:N)
- User likes Itinerary (N:M via Like)
- User comments on Itinerary (1:N)
- Itinerary contains Location (1:N)
- Location has Accommodation (1:N)
- Transport connects Locations (N:M)

---

## 2 Runtime View

### 2.1 Runtime Overview

#### Multi-Tenant Cloud Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        GOOGLE CLOUD PLATFORM                                 │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                     SHARED INFRASTRUCTURE                                ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    ││
│  │  │  Artifact   │  │    Cloud    │  │  Identity   │  │   Cloud     │    ││
│  │  │  Registry   │  │     DNS     │  │  Platform   │  │  Firestore  │    ││
│  │  │  (images)   │  │(tripico.fun)│  │ (tenants)   │  │  (shared)   │    ││
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                   SHARED GKE CLUSTER (tripico-cluster)                   ││
│  │                                                                          ││
│  │  ┌─────────────────────────────────────────────────────────────────┐    ││
│  │  │                    SHARED NAMESPACE                              │    ││
│  │  │  ┌─────────────────┐  ┌─────────────────┐                       │    ││
│  │  │  │ weather-service │  │ travel-warnings │                       │    ││
│  │  │  │ + postgres      │  │ + postgres      │                       │    ││
│  │  │  └─────────────────┘  └─────────────────┘                       │    ││
│  │  └─────────────────────────────────────────────────────────────────┘    ││
│  │                                                                          ││
│  │  ┌───────────────────────┐  ┌───────────────────────┐                   ││
│  │  │   FREEMIUM NAMESPACE  │  │   STANDARD NAMESPACE   │                  ││
│  │  │  ┌─────────────────┐  │  │  ┌─────────────────┐   │                  ││
│  │  │  │itinerary-service│  │  │  │itinerary-service│   │                  ││
│  │  │  │recommendation   │  │  │  │recommendation   │   │                  ││
│  │  │  │comments-likes   │  │  │  │comments-likes   │   │                  ││
│  │  │  │neo4j + postgres │  │  │  │neo4j + postgres │   │                  ││
│  │  │  └─────────────────┘  │  │  └─────────────────┘   │                  ││
│  │  │   Autoscaling: OFF    │  │   Autoscaling: ON      │                  ││
│  │  │   No tenant ID        │  │   Tenant: std-xxx      │                  ││
│  │  └───────────────────────┘  └───────────────────────┘                   ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │              ENTERPRISE GKE CLUSTER (tripico-{tenant}-cluster)          ││
│  │                                                                          ││
│  │  ┌─────────────────────────────────────────────────────────────────┐    ││
│  │  │                    DEFAULT NAMESPACE                             │    ││
│  │  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │    ││
│  │  │  │itinerary-service│  │ weather-service │  │ travel-warnings │  │    ││
│  │  │  │recommendation   │  │ (dedicated)     │  │ (dedicated)     │  │    ││
│  │  │  │comments-likes   │  │                 │  │                 │  │    ││
│  │  │  │neo4j + postgres │  │                 │  │                 │  │    ││
│  │  │  └─────────────────┘  └─────────────────┘  └─────────────────┘  │    ││
│  │  │   Autoscaling: ON (10 max)                                      │    ││
│  │  │   Tenant: ent-xxx                                               │    ││
│  │  │   Dedicated VPC, Static IP, DNS Wildcard                        │    ││
│  │  └─────────────────────────────────────────────────────────────────┘    ││
│  └─────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Cloud Resources

**Google Cloud Platform (GCP)**

Tripico operates with a multi-tenant architecture across GCP:

| Resource | Freemium | Standard | Enterprise |
|----------|----------|----------|------------|
| **GKE Cluster** | Shared (tripico-cluster) | Shared (tripico-cluster) | Dedicated (tripico-{tenant}-cluster) |
| **Kubernetes Namespace** | freemium | {tenant-name} | default |
| **VPC Network** | Shared (tripico-network) | Shared | Dedicated (tripico-{tenant}-network) |
| **Static IP** | Shared | Shared | Dedicated |
| **DNS Records** | Shared wildcard | Shared wildcard | Dedicated wildcard (*.{tenant}.tripico.fun) |
| **Identity Platform** | Project-level auth | Dedicated tenant (std-{name}) | Dedicated tenant (ent-{name}) |
| **Storage Bucket** | Shared | Dedicated | Dedicated |
| **API Gateway** | Shared | Dedicated | Dedicated |

**Running Application URLs:**

**Production:**
- Frontend: https://frontend-freemium.tripico.fun
- Freemium APIs: https://itinerary-freemium.tripico.fun
- Standard APIs: https://itinerary-{tenant}.tripico.fun
- Enterprise APIs: https://itinerary.{tenant}.tripico.fun

**Development:**
- Frontend: https://dev.frontend-freemium.tripico.fun
- Freemium APIs: https://itinerary-freemium.dev.tripico.fun

#### Service Communication

**Synchronous Services:**
- All microservices expose REST APIs with tenant context headers
- Frontend communicates with backend via REST APIs with tenant ID in headers/config
- Backend services validate tenant context from JWT tokens

**Asynchronous Services:**
- Travel Warnings Service uses scheduled jobs (every 15 minutes)
- Weather Service uses scheduled jobs for daily forecast updates
- Email notifications sent asynchronously via Quarkus Mailer

### 2.2 Microservices

#### 2.2.1 Itinerary Service

**Description:**
Core backend service managing user accounts and travel itineraries with tenant-aware data isolation.

**Runtime Configuration:**
- **Container Image:** `europe-west1-docker.pkg.dev/{project}/docker-repo/itinerary-service`
- **Deployment:** Kubernetes Deployment per namespace
- **Resource Limits:**
  ```yaml
  # Freemium
  resources:
    requests: { memory: "512Mi", cpu: "250m" }
    limits: { memory: "1Gi", cpu: "500m" }

  # Standard/Enterprise
  resources:
    requests: { memory: "512Mi", cpu: "250m" }
    limits: { memory: "1Gi", cpu: "1000m" }
  ```

**Scalability:**
| Tier | Min Replicas | Max Replicas | Auto-scaling |
|------|-------------|--------------|--------------|
| Freemium | 1 | 1 | OFF |
| Standard | 1 | 2 | ON (CPU 70%) |
| Enterprise | 2 | 10 | ON (CPU 70%) |

**Multi-Tenancy Isolation:**
- **Data Isolation:** Tenant ID stored in JWT, validated on every request
- **Firestore Collections:** Prefixed with tenant ID for Standard/Enterprise
- **Database:** Separate PostgreSQL instance per namespace
- **User Scoping:** Users can only access resources within their tenant

**Security:**
- Firebase/Identity Platform JWT token validation with tenant verification
- Tenant ID extracted from token claims (`firebase.tenant`)
- Workload Identity for GCP service access

**External Connections:**
- PostgreSQL (in-cluster deployment)
- Google Identity Platform for authentication
- Recommendation Service for graph sync

---

#### 2.2.2 Recommendation Service

**Description:**
Graph-based recommendation engine using Neo4j with tenant-aware graph partitioning.

**Runtime Configuration:**
- **Deployment:** Kubernetes Deployment + StatefulSet for Neo4j
- **Resource Limits:**
  ```yaml
  # Neo4j
  resources:
    requests: { memory: "1Gi", cpu: "500m" }
    limits: { memory: "2Gi", cpu: "2000m" }
  ```

**Multi-Tenancy Isolation:**
- **Graph Partitioning:** Each tenant's data stored with tenant prefix on node properties
- **Query Isolation:** All Cypher queries filtered by tenant ID
- **Namespace Separation:** Separate Neo4j instance per namespace

**Graph Query with Tenant Filter:**
```cypher
// Recommendations filtered by tenant
MATCH (user:User {userId: $userId, tenantId: $tenantId})
      -[:LIKES]->(itinerary:Itinerary {tenantId: $tenantId})
      <-[:LIKES]-(similarUser:User {tenantId: $tenantId})
MATCH (similarUser)-[:LIKES]->(recommendation:Itinerary {tenantId: $tenantId})
WHERE NOT (user)-[:LIKES]->(recommendation)
RETURN recommendation
ORDER BY recommendation.likesCount DESC
```

---

#### 2.2.3 Comments & Likes Service

**Description:**
Handles social interactions on itineraries using Firestore with tenant-aware collections.

**Multi-Tenancy Isolation:**
- **Collection Structure:**
  ```
  # Freemium (no tenant prefix)
  /comments/{commentId}
  /likes/{likeId}

  # Standard/Enterprise (tenant-prefixed)
  /tenants/{tenantId}/comments/{commentId}
  /tenants/{tenantId}/likes/{likeId}
  ```
- **Security Rules:** Firestore security rules enforce tenant isolation
- **Query Filtering:** All queries include tenant ID filter

---

#### 2.2.4 Travel Warnings Service (Shared)

**Description:**
Fetches travel advisories from Auswartiges Amt API. Deployed in shared namespace for Freemium/Standard, dedicated for Enterprise.

**Multi-Tenancy:**
- **Freemium/Standard:** Shared service in `shared` namespace
- **Enterprise:** Dedicated instance in enterprise cluster

---

#### 2.2.5 Weather Forecast Service (Shared)

**Description:**
Integrates with Meteosource API for weather forecasts. Shared across Freemium/Standard tiers.

**Multi-Tenancy:**
- **Freemium/Standard:** Shared service in `shared` namespace
- **Enterprise:** Dedicated instance in enterprise cluster

---

### 2.3 Datastores

#### Storage Overview with Tenant Isolation

| Datastore | Technology | Freemium | Standard | Enterprise |
|-----------|------------|----------|----------|------------|
| **Itinerary DB** | PostgreSQL | Shared instance | Namespace-isolated | Cluster-isolated |
| **Comments/Likes** | Firestore | Shared collections | Tenant-prefixed collections | Tenant-prefixed collections |
| **Recommendations** | Neo4j | Shared graph | Namespace-isolated | Cluster-isolated |
| **Weather/Warnings** | PostgreSQL | Shared | Shared | Dedicated |

#### Firestore Multi-Tenant Structure

```json
// Collection: /tenants/{tenantId}/comments
{
  "id": "comment-123",
  "tenantId": "std-tenant-1",
  "itineraryId": "itin-456",
  "userId": "user-789",
  "userName": "John Doe",
  "content": "Great trip!",
  "createdAt": "2026-01-15T10:30:00Z"
}

// Collection: /tenants/{tenantId}/likes
{
  "id": "like-123",
  "tenantId": "std-tenant-1",
  "itineraryId": "itin-456",
  "userId": "user-789",
  "createdAt": "2026-01-15T10:30:00Z"
}
```

#### PostgreSQL Tenant Isolation

**Namespace-Level Isolation (Standard):**
- Each Standard tenant namespace has its own PostgreSQL deployment
- Database per namespace: `itinerary_db` in each tenant's namespace
- No cross-namespace data access possible

**Cluster-Level Isolation (Enterprise):**
- Dedicated PostgreSQL instance in dedicated GKE cluster
- Complete network isolation via dedicated VPC
- No shared resources with other tenants

---

### 2.4 Security: Roles and Role Mapping

#### Service Accounts

**GCP Service Account:** `tripico-sa@{project-id}.iam.gserviceaccount.com`

| Role | Purpose |
|------|---------|
| `roles/datastore.user` | Firestore read/write |
| `roles/storage.objectViewer` | Cloud Storage read |
| `roles/secretmanager.secretAccessor` | Secret access |
| `roles/iam.workloadIdentityUser` | Workload Identity binding |

**Workload Identity Bindings:**

Each Kubernetes service account is bound to the GCP service account with tenant-specific bindings:

```hcl
# Freemium namespace
serviceAccount:{project}.svc.id.goog[freemium/itinerary-service-sa]

# Standard namespace
serviceAccount:{project}.svc.id.goog[{tenant-name}/itinerary-service-sa]

# Enterprise (dedicated cluster)
serviceAccount:{project}.svc.id.goog[default/itinerary-service-sa]
```

#### Identity Platform Multi-Tenancy

**Tenant Structure:**
```
Google Identity Platform (Project Level)
├── Project-level users (Freemium - no tenant)
├── Tenant: std-standard-1
│   └── Users isolated to this tenant
├── Tenant: std-standard-2
│   └── Users isolated to this tenant
├── Tenant: ent-enterprise-1
│   └── Users isolated to this tenant
└── Tenant: ent-acme-corp
    └── Users isolated to this tenant
```

**Authentication Flow with Tenant Isolation:**

```
┌──────────────┐     ┌─────────────────┐     ┌──────────────────┐
│   Frontend   │────>│ Identity Platform│────>│  Backend Service │
│              │     │                 │     │                  │
│ 1. User logs │     │ 2. Validates    │     │ 3. Extracts      │
│    in with   │     │    credentials  │     │    tenant ID     │
│    tenant    │     │    for tenant   │     │    from JWT      │
│    context   │     │                 │     │                  │
│              │     │ Returns JWT     │     │ 4. Filters data  │
│              │     │ with tenant ID  │     │    by tenant     │
└──────────────┘     └─────────────────┘     └──────────────────┘
```

**JWT Token Claims for Multi-Tenancy:**
```json
{
  "iss": "https://securetoken.google.com/{project-id}",
  "sub": "user-uid-123",
  "firebase": {
    "tenant": "std-standard-1",
    "sign_in_provider": "password"
  },
  "email": "user@example.com"
}
```

**Backend Tenant Validation (Quarkus):**
```java
@Provider
public class TenantContextFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext ctx) {
        // Extract tenant from JWT
        String expectedTenant = config.getOptionalValue(
            "identity-platform.tenant.expected-id", String.class)
            .orElse(null);

        // Freemium: no tenant expected
        if (expectedTenant == null || expectedTenant.isEmpty()) {
            return; // Allow project-level auth
        }

        // Standard/Enterprise: validate tenant matches
        String tokenTenant = extractTenantFromJwt(ctx);
        if (!expectedTenant.equals(tokenTenant)) {
            throw new ForbiddenException("Tenant mismatch");
        }
    }
}
```

**Tenant Isolation Enforcement:**

| Layer | Isolation Mechanism |
|-------|---------------------|
| **Network** | Kubernetes NetworkPolicies, Enterprise VPC isolation |
| **Authentication** | Identity Platform tenant separation |
| **Authorization** | JWT tenant claim validation in every service |
| **Data** | Tenant-prefixed collections, namespace-isolated databases |
| **Compute** | Namespace isolation, Enterprise cluster isolation |

---
