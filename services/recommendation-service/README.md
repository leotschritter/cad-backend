# Recommendation Service

The Recommendation Service is a microservice responsible for generating personalized travel feed recommendations using **graph-based collaborative filtering** based on the **Personalized Live Feed Epic**.

## Overview

This service uses a Neo4j graph database to model relationships between users, itineraries, and locations, enabling sophisticated recommendation algorithms based on:
- **Collaborative filtering**: "Users who liked what you liked also liked..."
- **Location-based recommendations**: Itineraries containing locations you've visited
- **Social signals**: Popularity based on likes count
- **Automatic fallback**: Popular content for new users

## Graph Database Schema

The service models the following graph structure in Neo4j:

```
(User)-[:LIKES]->(Itinerary)
(User)-[:CREATED]->(Itinerary)
(User)-[:VISITED]->(Location)
(Itinerary)-[:INCLUDES]->(Location)
```

### Node Types
- **User**: Represents a traveller
- **Itinerary**: Represents a travel itinerary
- **Location**: Represents a destination/location

### Relationship Types
- **LIKES**: User likes an itinerary (created when user clicks like in frontend)
- **CREATED**: User created an itinerary
- **VISITED**: User visited a location (from itinerary destinations)
- **INCLUDES**: Itinerary includes a location

## Features

### Story 1: See and Explore Suggestions on Feed Page
- Provides a paginated feed endpoint
- Returns itinerary cards with: title, description, traveller name, and likes count
- Items ordered by relevance score (combining collaborative filtering and popularity)

### Story 2: Discover Itineraries from Travellers Who Visited the Same Places
- Uses graph queries to find itineraries containing shared destinations
- Includes match reason labels explaining why something was recommended
- Prioritizes content from users with similar travel patterns

### Story 3: Refine Recommendation Algorithm with Social Signals and Basic Feed
- Incorporates likes count as a key relevance signal
- Automatically falls back to popular feed for new travellers
- Balances personalization with popularity using weighted relevance scoring

## API Endpoints

### Feed Endpoints

#### GET /api/v1/feed
Get personalized feed for a traveller using collaborative filtering.

**Query Parameters:**
- `travellerId` (required): The ID of the traveller
- `page` (optional, default: 0): Page number (0-based)
- `pageSize` (optional, default: 20): Number of items per page

**Algorithm:**
1. Find users who liked the same itineraries as you
2. Find what else those users liked
3. Supplement with location-based recommendations
4. Fall back to popular content if needed

**Response:**
```json
{
  "items": [
    {
      "itineraryId": 123,
      "title": "Amazing Trip to Paris",
      "description": "Explored the city of lights...",
      "travellerName": "John Doe",
      "likesCount": 45,
      "destinations": ["Paris", "Versailles"],
      "matchReason": "Liked by 5 users with similar taste",
      "relevanceScore": 8.5
    }
  ],
  "page": 0,
  "pageSize": 20,
  "totalItems": 100,
  "hasMore": true
}
```

#### GET /feed/popular
Get popular feed with most liked itineraries.

**Query Parameters:**
- `page` (optional, default: 0): Page number (0-based)
- `pageSize` (optional, default: 20): Number of items per page

### Graph Event Endpoints

These endpoints should be called from the frontend when users perform actions:

#### POST /graph/like
Record a like action. **Call this when a user likes an itinerary.**

**Request Body:**
```json
{
  "userId": "user123",
  "itineraryId": 456
}
```

#### DELETE /graph/like
Remove a like action. **Call this when a user unlikes an itinerary.**

**Request Body:**
```json
{
  "userId": "user123",
  "itineraryId": 456
}
```

#### POST /graph/itinerary
Record an itinerary creation or update. **Call this when a user creates/updates an itinerary.**

**Request Body:**
```json
{
  "itineraryId": 789,
  "userId": "user123",
  "title": "Trip to Japan",
  "description": "Amazing journey...",
  "locationNames": ["Tokyo", "Kyoto", "Osaka"],
  "likesCount": 10
}
```

#### POST /graph/locations
Record location visits. **Call this when a user adds locations to an itinerary.**

**Request Body:**
```json
{
  "userId": "user123",
  "locationNames": ["Tokyo", "Kyoto"]
}
```

## Technology Stack

- **Java 21**
- **Quarkus 3.28.1** - Supersonic Subatomic Java Framework
- **Maven** - Dependency management and build tool
- **Neo4j 5.25** - Graph database for storing relationships
- **Quarkus Neo4j Extension** - Neo4j driver integration
- **REST Client** - For calling the Itinerary Service
- **SmallRye OpenAPI** - API documentation
- **SmallRye Health** - Health checks for Kubernetes

## Project Structure

```
recommendation-service/
├── src/
│   ├── main/
│   │   ├── java/de/htwg/
│   │   │   ├── api/
│   │   │   │   ├── FeedResource.java     # Feed API endpoints
│   │   │   │   └── GraphResource.java    # Graph event recording
│   │   │   ├── service/
│   │   │   │   ├── RecommendationService.java  # Recommendation logic
│   │   │   │   └── GraphService.java           # Graph operations
│   │   │   ├── dto/                      # Data Transfer Objects
│   │   │   │   ├── FeedItemDTO.java
│   │   │   │   ├── FeedResponseDTO.java
│   │   │   │   ├── LikeActionDTO.java
│   │   │   │   ├── ItineraryEventDTO.java
│   │   │   │   └── LocationVisitDTO.java
│   │   │   └── health/                   # Health checks
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/de/htwg/
└── pom.xml
```

## Configuration

The service can be configured via environment variables:

### Neo4j Database
- `NEO4J_URI` - Neo4j connection URI (default: bolt://localhost:7687)
- `NEO4J_USERNAME` - Neo4j username (default: neo4j)
- `NEO4J_PASSWORD` - Neo4j password (default: password)

### External Services
- `ITINERARY_SERVICE_URL` - URL of the Itinerary Service (default: http://localhost:8080)
- `FIREBASE_CREDENTIALS_PATH` - Path to Firebase credentials file

### Application
- Port: `8083` (default)
- Default page size: `20`
- Max page size: `100`

## Development

### Prerequisites
- Java 21
- Maven 3.9+
- Neo4j database

### Running Locally with Docker Compose

The easiest way to get started is with Docker Compose, which includes Neo4j:

```bash
docker-compose up -d
```

This starts:
- Neo4j on ports 7474 (HTTP) and 7687 (Bolt)
- Recommendation service on port 8083

### Running in Development Mode

1. Start Neo4j:
```bash
docker run -d \
  --name neo4j \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/password \
  neo4j:5.25-community
```

2. Set up environment variables:
```bash
export NEO4J_URI=bolt://localhost:7687
export NEO4J_USERNAME=neo4j
export NEO4J_PASSWORD=password
export ITINERARY_SERVICE_URL=http://localhost:8080
export FIREBASE_CREDENTIALS_PATH=/path/to/firebase-credentials.json
```

3. Run in development mode:
```bash
./mvnw quarkus:dev
```

4. Access the application:
- API: http://localhost:8083/feed
- Swagger UI: http://localhost:8083/swagger-ui
- Health: http://localhost:8083/q/health
- Neo4j Browser: http://localhost:7474

### Exploring the Graph

You can explore and query the graph using Neo4j Browser (http://localhost:7474):

```cypher
// View all nodes and relationships
MATCH (n) RETURN n LIMIT 25

// Find popular itineraries
MATCH (i:Itinerary)
RETURN i.title, SIZE((i)<-[:LIKES]-()) as likes
ORDER BY likes DESC

// Find users with similar tastes
MATCH (u1:User {id: 'user123'})-[:LIKES]->(i)<-[:LIKES]-(u2:User)
RETURN u2.id, COUNT(i) as commonLikes
ORDER BY commonLikes DESC

// Find itineraries with shared locations
MATCH (u:User {id: 'user123'})-[:VISITED]->(loc:Location)<-[:INCLUDES]-(i:Itinerary)
RETURN i.title, COUNT(loc) as sharedLocations
ORDER BY sharedLocations DESC
```

### Building

Build the application:
```bash
./mvnw clean package
```

Build Docker image:
```bash
docker build -f Dockerfile -t recommendation-service .
```

### Testing

Run tests:
```bash
./mvnw test
```

## Recommendation Algorithms

### 1. Collaborative Filtering
Finds users who liked similar itineraries and recommends what they also liked:

```cypher
MATCH (u:User {id: $userId})-[:LIKES]->(i:Itinerary)<-[:LIKES]-(other:User)
MATCH (other)-[:LIKES]->(recommendation:Itinerary)
WHERE NOT (u)-[:LIKES]->(recommendation)
RETURN recommendation
ORDER BY COUNT(other) DESC
```

**Relevance Score:** `commonUsers * 2.0 + totalLikes * 0.5`

### 2. Location-Based
Recommends itineraries containing locations the user has visited:

```cypher
MATCH (u:User {id: $userId})-[:VISITED]->(loc:Location)<-[:INCLUDES]-(i:Itinerary)
WHERE NOT (u)-[:LIKES]->(i)
RETURN i, COUNT(loc) as commonLocations
ORDER BY commonLocations DESC
```

**Relevance Score:** `commonLocations * 3.0 + totalLikes * 0.3`

### 3. Popular Feed (Fallback)
Shows most liked itineraries for users with no personalization data:

```cypher
MATCH (i:Itinerary)
RETURN i, SIZE((i)<-[:LIKES]-()) as likes
ORDER BY likes DESC
```

## Frontend Integration

### When a User Likes an Itinerary

```javascript
// POST to record the like in the graph
await fetch('http://localhost:8083/graph/like', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    userId: currentUser.id,
    itineraryId: itinerary.id
  })
});
```

### When a User Creates/Updates an Itinerary

```javascript
// POST to record the itinerary
await fetch('http://localhost:8083/graph/itinerary', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    itineraryId: itinerary.id,
    userId: currentUser.id,
    title: itinerary.title,
    description: itinerary.description,
    locationNames: itinerary.locations.map(l => l.name),
    likesCount: itinerary.likesCount
  })
});
```

### When a User Adds Locations to an Itinerary

```javascript
// POST to record visited locations
await fetch('http://localhost:8083/graph/locations', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    userId: currentUser.id,
    locationNames: ['Paris', 'Rome', 'Barcelona']
  })
});
```

### Getting Personalized Recommendations

```javascript
// GET personalized feed
const response = await fetch(
  `http://localhost:8083/feed?travellerId=${currentUser.id}&page=0&pageSize=20`
);
const feed = await response.json();
```

## Deployment

See [k8s/README.md](k8s/README.md) for Kubernetes deployment instructions.

Key changes for Neo4j deployment:
1. Deploy Neo4j instance or use managed service
2. Create secret for Neo4j credentials:
```bash
kubectl create secret generic neo4j-secret \
  --from-literal=username=neo4j \
  --from-literal=password=YOUR_PASSWORD
```

## Performance Considerations

- **Indexing**: Create indexes on frequently queried properties:
  ```cypher
  CREATE INDEX user_id FOR (u:User) ON (u.id);
  CREATE INDEX itinerary_id FOR (i:Itinerary) ON (i.id);
  CREATE INDEX location_name FOR (l:Location) ON (l.name);
  ```

- **Connection Pooling**: Configured in application.properties with max 50 connections

- **Query Optimization**: All queries use MERGE to avoid duplicates and include proper WHERE clauses

## Contributing

When contributing to this service, please ensure:
- All tests pass
- Graph queries are optimized
- API changes are documented
- User stories are referenced in commits

## License

[Add your license information here]

