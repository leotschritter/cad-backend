# 🚀 Multi-Service Load Testing Suite

## Overview

This comprehensive load testing suite tests all microservices in the travel application:
- **Itinerary Service**: Core CRUD operations for travel itineraries
- **Comments & Likes Service**: Social interactions (likes and comments)
- **Recommendation Service**: Personalized feed and graph database operations
- **Weather Forecast Service**: Weather data retrieval
- **Travel Warnings Service**: Travel advisory information

The tests simulate realistic user behavior patterns across all services with coordinated requests that mirror production usage.

## Architecture

The load tests follow these key principles:

1. **Coordinated Operations**: Likes and itinerary creation trigger graph updates in the recommendation service
2. **Realistic Transaction Mix**: Different user behavior patterns for periodic vs viral scenarios
3. **Proper Data Seeding**: Pre-populate services with test data to simulate production conditions
4. **Authentication Support**: Compatible with both Firebase-enabled and local deployments

## Test Scenarios

### 5.1 Periodic Workload Tests

Simulates normal daily traffic patterns with varying concurrency:

**Scenario 1: Moderate Load**
- 100 concurrent users during peak times
- 10 concurrent users during low demand times
- Duration: 20 minutes (5 min ramp-up, 10 min sustained, 5 min ramp-down)

**Scenario 2: High Load**
- 1000 concurrent users during peak times
- 20 concurrent users during low demand times
- Duration: 30 minutes (7 min ramp-up, 16 min sustained, 7 min ramp-down)

**Transaction Mix**:
- 50% Browse/Search operations (viewing itineraries, searching, weather, travel warnings)
- 25% Social interactions (likes, comments, viewing details)
- 15% Recommendation service (personalized feed, popular content)
- 8% Content creation (new itineraries, locations)
- 2% User profile operations

### 5.2 Once-in-a-Lifetime Workload Tests

Simulates traffic spikes from viral content or major events:

**Base Load**: 10 concurrent users
**Growth**: Constant addition of new users (configurable rate)

**Test Goals**:
- Determine maximum workload without degradation
- Identify degradation threshold
- Find breaking point of the system

**Transaction Mix** (focused on viral behavior):
- 70% Heavy reads on popular content (hot spots)
- 20% Social interactions (burst of likes/comments on viral content)
- 7% Discovery/search operations
- 3% Content creation

## Configuration

### Initial Data Seeding

Before running tests, the application is seeded with:
- **Users**: 1000 test users (format: `loadtest.{id}@example.com`)
- **Itineraries**: 3 per user (3000 total)
- **Locations**: 2 per itinerary (6000 total)
- **Weather Data**: Pre-cached for major destinations
- **Graph Data**: Users, locations, and itineraries in Neo4j

This ensures:
- Realistic database sizes
- Proper indexing behavior
- Cache performance testing
- No cold-start effects

### Environment Variables

All configuration is done via `.env` files:

**`.env.seed`** - Data seeding configuration
```env
# Service URLs
ITINERARY_SERVICE_URL=https://api.example.com
RECOMMENDATION_SERVICE_URL=https://api.example.com
WEATHER_SERVICE_URL=https://api.example.com
TRAVEL_WARNINGS_SERVICE_URL=https://api.example.com

# Seeding parameters
NUM_USERS=1000
NUM_ITINERARIES_PER_USER=3
NUM_LOCATIONS_PER_ITINERARY=2

# Authentication
CREATE_FIREBASE_USERS=true
FIREBASE_PASSWORD=LoadTest123!
FIREBASE_API_KEY=your-api-key
```

**`.env.periodic`** - Periodic workload test
```env
ITINERARY_SERVICE_URL=https://api.example.com
RECOMMENDATION_SERVICE_URL=https://api.example.com
WEATHER_SERVICE_URL=https://api.example.com
TRAVEL_WARNINGS_SERVICE_URL=https://api.example.com

# Locust parameters
USERS=100
SPAWN_RATE=5
RUN_TIME=20m

# Authentication
FIREBASE_API_KEY=your-api-key
```

**`.env.onceinlifetime`** - Viral/spike workload test
```env
ITINERARY_SERVICE_URL=https://api.example.com
RECOMMENDATION_SERVICE_URL=https://api.example.com
WEATHER_SERVICE_URL=https://api.example.com
TRAVEL_WARNINGS_SERVICE_URL=https://api.example.com

# Locust parameters
BASE_USERS=10
MAX_USERS=2000
USER_GROWTH_RATE=10
SPAWN_RATE=10
RUN_TIME=30m

# Authentication
FIREBASE_API_KEY=your-api-key
```

## Quick Start

### Prerequisites

- Python 3.10+
- Access to deployed services or local development environment
- (Optional) Firebase API key for authentication tests

### Installation

```bash
cd load/
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 1. Seed Test Data

```bash
python seed_data.py
```

This creates all necessary test data across all services.

### 2. Run Periodic Workload Tests

**Scenario 1: 100 peak users**
```bash
./run_periodic_test.sh --scenario 1
```

**Scenario 2: 1000 peak users**
```bash
./run_periodic_test.sh --scenario 2
```

### 3. Run Once-in-a-Lifetime Tests

```bash
./run_onceinlifetime_test.sh
```

### 4. View Reports

Reports are generated in `./reports/` directory:
- `periodic_scenario1_report.html`
- `periodic_scenario2_report.html`
- `onceinlifetime_report.html`

### 5. (Optional) Cleanup Test Data

```bash
python cleanup.py
```

⚠️ **Warning**: This removes all test users and data. Only run after testing is complete.

## Complete Test Suite

Run everything in sequence:

```bash
./run_complete_loadtest.sh
```

This script:
1. Sets up Python virtual environment
2. Installs dependencies
3. Seeds test data
4. Runs periodic scenario 1 (100 users)
5. Runs periodic scenario 2 (1000 users)
6. Runs once-in-a-lifetime test
7. Generates all reports
8. (Optional) Cleans up test data

## Output & Metrics

### Response Time Analysis
- **p50 (median)**: 50th percentile response time
- **p95**: 95th percentile response time
- **p99**: 99th percentile response time
- **Average**: Mean response time
- **Min/Max**: Range of response times

### Failure Rate Analysis
- **Success Rate**: Percentage of successful requests
- **Failure Rate**: Percentage of failed requests
- **Error Types**: Breakdown by error code

### Resource Utilization
Monitor via Google Cloud Console or Kubernetes metrics:
- **CPU Usage**: Per service
- **Memory Usage**: Per service
- **Database Connections**: PostgreSQL, Firestore, Neo4j
- **Cache Hit Rates**: Redis/Memcached if applicable
- **Network I/O**: Request/response sizes

### Throughput Metrics
- **Requests/second**: Total and per endpoint
- **Concurrent Users**: Active users over time
- **Response Time Distribution**: Histogram over test duration

## Test Duration Justification

### Periodic Tests
- **Ramp-up**: 5-7 minutes to gradually increase load, avoiding cold start effects
- **Sustained**: 10-16 minutes to observe steady-state performance
- **Ramp-down**: 5-7 minutes to safely decrease load
- **Total**: 20-30 minutes per scenario

This duration ensures:
- JIT compilation completes
- Caches warm up properly
- Connection pools stabilize
- Database query plans optimize
- Long enough to detect memory leaks

### Once-in-a-Lifetime Tests
- **Base Load**: 5 minutes to establish baseline
- **Growth Phase**: 15-20 minutes of constant user growth
- **Sustained Peak**: 5 minutes at maximum load
- **Ramp-down**: 5 minutes
- **Total**: 30-35 minutes

This duration ensures:
- Clear identification of degradation point
- Time to recover from transient issues
- Observation of cascading failures
- Resource exhaustion detection

## Data Cleanup Considerations

### Should You Clean Up After Tests?

**Pros of Cleanup**:
- ✅ Prevents database bloat
- ✅ Ensures consistent baseline for repeated tests
- ✅ Removes test users from authentication systems

**Cons of Cleanup**:
- ❌ May want to inspect data post-test
- ❌ Time-consuming for large datasets
- ❌ Risk of deleting non-test data if filters are wrong

### Recommendation

**For Development/Testing Environments**: 
- Run cleanup after each test cycle
- Use isolated test databases if possible

**For Staging Environments**:
- Keep test data for analysis
- Clean up periodically (weekly/monthly)
- Use database snapshots before cleanup

**For Production**:
- Never run load tests on production
- Use production-like staging environment instead

### Safe Cleanup

The `cleanup.py` script only removes:
- Users matching pattern `loadtest.*@example.com`
- Itineraries created by test users
- Locations belonging to test itineraries
- Likes and comments from test users
- Graph nodes for test entities

It does **NOT** touch:
- Real user data
- System configuration
- Schema definitions

## Troubleshooting

### Tests Fail to Start

**Check service URLs**:
```bash
curl -I $ITINERARY_SERVICE_URL/health
```

**Verify authentication**:
```bash
# Test Firebase token generation
python -c "from auth_helper import get_bearer_token; print(get_bearer_token())"
```

### High Failure Rates

- Check service logs for errors
- Verify database connections
- Ensure sufficient resources (CPU/memory)
- Check rate limiting configuration

### Slow Performance

- Verify database indexes exist
- Check for N+1 query problems
- Monitor database query times
- Review connection pool sizes

### Memory Issues

- Increase service memory limits
- Check for memory leaks
- Verify connection pools release properly
- Monitor garbage collection

## Architecture Decisions

### Why Coordinate Likes with Recommendation Service?

In production, when a user likes an itinerary, the system should:
1. Record the like in the Comments & Likes service (Firestore)
2. Update the social graph in the Recommendation service (Neo4j)

This coordination ensures:
- Personalized feeds reflect user preferences
- Graph-based recommendations stay current
- Social signals propagate correctly

The load tests mirror this by calling both endpoints for realistic behavior.

### Why Pre-seed Data?

Benefits:
- **Realistic Performance**: Tests hit warm caches and optimized indexes
- **Consistent Baselines**: Each test run starts from same state
- **Complex Scenarios**: Can test searches, recommendations, social features
- **Production-Like**: Mimics actual system state

### Why Different Transaction Mixes?

**Periodic (Normal)**: 
- More reads than writes (typical SaaS application)
- Balanced across all services
- Realistic user behavior

**Once-in-a-Lifetime (Viral)**:
- Heavy concentration on hot content (cache testing)
- Burst of social interactions (write spike testing)
- Tests system under unusual load patterns

## Results Analysis Template

### Periodic Workload - Scenario 1 (100 users)

**Test Configuration**:
- Peak Users: 100
- Low Users: 10
- Duration: 20 minutes
- Initial Data: 1000 users, 3000 itineraries

**Results**:
| Endpoint | p50 | p95 | p99 | Avg RPS | Failure Rate |
|----------|-----|-----|-----|---------|--------------|
| POST /itinerary/search | X ms | X ms | X ms | X | X% |
| GET /feed | X ms | X ms | X ms | X | X% |
| ... | ... | ... | ... | ... | ... |

**Resource Utilization**:
| Service | Avg CPU | Peak CPU | Avg Memory | Peak Memory |
|---------|---------|----------|------------|-------------|
| Itinerary | X% | X% | X MB | X MB |
| ... | ... | ... | ... | ... |

**Analysis**:
- [Your observations here]
- [Performance bottlenecks]
- [Recommendations]

### Once-in-a-Lifetime Workload

**Test Configuration**:
- Base Load: 10 users
- Growth Rate: 10 users/minute
- Duration: 30 minutes
- Maximum Users Reached: X

**Degradation Points**:
- **No Degradation**: 0-X users (response times < 500ms)
- **With Degradation**: X-Y users (response times 500ms-2s, some failures)
- **Breaking Point**: Y+ users (response times > 2s, high failure rate)

**Analysis**:
- [Maximum sustainable load]
- [Failure modes observed]
- [Resource bottlenecks]
- [Recommendations for scaling]

## Contributing

When adding new services or endpoints:
1. Update `seed_data.py` to seed data for the new service
2. Add tasks to `locustfile_periodic.py` with appropriate weights
3. Add tasks to `locustfile_onceinlifetime.py` for viral scenarios
4. Update this README with new transaction mix percentages
5. Add cleanup logic if needed

## License

[Your License Here]

