#!/bin/bash

set -e  # Exit on error

echo "🚀 Starting Microservices Migration..."
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Confirmation
echo -e "${YELLOW}⚠️  This script will restructure your repository!${NC}"
echo "   - Move files to services/ directory"
echo "   - Update Dockerfile paths"
echo "   - Reorganize kubernetes charts"
echo ""
read -p "Do you want to continue? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Migration cancelled."
    exit 0
fi

# Create backup branch
echo ""
echo "📦 Creating backup branch..."
git checkout -b backup-before-migration-$(date +%Y%m%d-%H%M%S)
git checkout -b feature/microservices-restructure

echo -e "${GREEN}✅ Backup branch created${NC}"

# Create services directory structure
echo ""
echo "📁 Creating services directory structure..."
mkdir -p services/{itinerary-service,weather-forecast-service,travel-warnings-service}

echo -e "${GREEN}✅ Directory structure created${NC}"

# Move Itinerary Service (main project)
echo ""
echo "📦 Migrating Itinerary Service..."
if [ -d "src" ]; then
    mv src services/itinerary-service/
    echo "  ✅ Moved src/"
fi
if [ -f "pom.xml" ]; then
    mv pom.xml services/itinerary-service/
    echo "  ✅ Moved pom.xml"
fi
if [ -f "Dockerfile" ]; then
    mv Dockerfile services/itinerary-service/
    echo "  ✅ Moved Dockerfile"
fi
if [ -f "mvnw" ]; then
    mv mvnw services/itinerary-service/
    echo "  ✅ Moved mvnw"
fi
if [ -f "mvnw.cmd" ]; then
    mv mvnw.cmd services/itinerary-service/
    echo "  ✅ Moved mvnw.cmd"
fi
if [ -d ".mvn" ]; then
    cp -r .mvn services/itinerary-service/
    echo "  ✅ Copied .mvn/"
fi

# Move Weather Forecast Service
echo ""
echo "🌤️  Migrating Weather Forecast Service..."
if [ -d "weather-forecast-service" ]; then
    cp -r weather-forecast-service/* services/weather-forecast-service/ 2>/dev/null || true
    echo "  ✅ Copied weather-forecast-service files"
fi
if [ -f "Weather.Dockerfile" ]; then
    mv Weather.Dockerfile services/weather-forecast-service/Dockerfile
    echo "  ✅ Moved & renamed Weather.Dockerfile"
fi

# Move Travel Warnings Service
echo ""
echo "⚠️  Migrating Travel Warnings Service..."
if [ -d "travel_warnings" ]; then
    cp -r travel_warnings/* services/travel-warnings-service/ 2>/dev/null || true
    echo "  ✅ Copied travel_warnings files"
fi
if [ -f "TravelWarnings.Dockerfile" ]; then
    mv TravelWarnings.Dockerfile services/travel-warnings-service/Dockerfile
    echo "  ✅ Moved & renamed TravelWarnings.Dockerfile"
fi

# Reorganize Kubernetes charts
echo ""
echo "☸️  Reorganizing Kubernetes charts..."
if [ -d "kubernetes/itinerary-service" ]; then
    mv kubernetes/itinerary-service kubernetes/itinerary-service-chart-old
    echo "  ✅ Backed up old itinerary-service"
fi

if [ -d "services/weather-forecast-service/helm" ]; then
    mv services/weather-forecast-service/helm kubernetes/weather-forecast-service-chart
    echo "  ✅ Moved weather helm chart"
fi

if [ -d "services/weather-forecast-service/k8s" ]; then
    mv services/weather-forecast-service/k8s kubernetes/weather-forecast-service-manifests
    echo "  ✅ Moved weather k8s manifests"
fi

if [ -d "services/travel-warnings-service/travel-warnings-chart" ]; then
    mv services/travel-warnings-service/travel-warnings-chart kubernetes/travel-warnings-service-chart
    echo "  ✅ Moved travel-warnings helm chart"
fi

# Create README files for each service
echo ""
echo "📝 Creating README files..."

cat > services/itinerary-service/README.md <<EOF
# Itinerary Service

Main backend service for managing travel itineraries.

## Build

\`\`\`bash
cd services/itinerary-service
./mvnw clean package
\`\`\`

## Docker Build

\`\`\`bash
docker build -t itinerary-service:latest .
\`\`\`

## Run

\`\`\`bash
./mvnw quarkus:dev
\`\`\`
EOF

cat > services/weather-forecast-service/README.md <<EOF
# Weather Forecast Service

Service for providing weather forecast information.

## Build

\`\`\`bash
cd services/weather-forecast-service
./mvnw clean package
\`\`\`

## Docker Build

\`\`\`bash
docker build -t weather-service:latest .
\`\`\`
EOF

cat > services/travel-warnings-service/README.md <<EOF
# Travel Warnings Service

Service for managing travel warnings and safety information.

## Build

\`\`\`bash
cd services/travel-warnings-service
./mvnw clean package
\`\`\`

## Docker Build

\`\`\`bash
docker build -t travel-warnings-service:latest .
\`\`\`
EOF

echo -e "${GREEN}✅ README files created${NC}"

# Create root README
echo ""
echo "📝 Updating root README..."

cat > README-SERVICES.md <<EOF
# Tripico Backend Services

Microservices architecture for the Tripico travel platform.

## Services

### 1. Itinerary Service
- **Path**: \`services/itinerary-service/\`
- **Description**: Main service for managing travel itineraries
- **Port**: 8080
- **Tech Stack**: Java 21, Quarkus, PostgreSQL

### 2. Weather Forecast Service
- **Path**: \`services/weather-forecast-service/\`
- **Description**: Weather forecast and climate information
- **Port**: 8081
- **Tech Stack**: Java 21, Quarkus

### 3. Travel Warnings Service
- **Path**: \`services/travel-warnings-service/\`
- **Description**: Travel warnings and safety information
- **Port**: 8082
- **Tech Stack**: Java 21, Quarkus

## Quick Start

### Build all services
\`\`\`bash
# Itinerary
cd services/itinerary-service && ./mvnw clean package

# Weather
cd services/weather-forecast-service && ./mvnw clean package

# Travel Warnings
cd services/travel-warnings-service && ./mvnw clean package
\`\`\`

### Docker Build
\`\`\`bash
# Itinerary
docker build -t itinerary-service:latest services/itinerary-service/

# Weather
docker build -t weather-service:latest services/weather-forecast-service/

# Travel Warnings
docker build -t travel-warnings-service:latest services/travel-warnings-service/
\`\`\`

## Deployment

See \`kubernetes/\` directory for Helm charts and deployment configurations.

## Migration

This repository was migrated from a monolithic structure to microservices.
See \`MIGRATION_PLAN.md\` for details.
EOF

echo -e "${GREEN}✅ Root README created${NC}"

# Git status
echo ""
echo "📊 Current git status:"
git status --short

# Summary
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ Migration Complete!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "📁 New Structure:"
echo "  services/"
echo "  ├── itinerary-service/"
echo "  ├── weather-forecast-service/"
echo "  └── travel-warnings-service/"
echo ""
echo "⚠️  Next Steps:"
echo "  1. Review changes: git status"
echo "  2. Test builds in each service"
echo "  3. Update CI/CD pipelines (.github/workflows/)"
echo "  4. Update Kubernetes charts if needed"
echo "  5. Commit: git add . && git commit -m 'Migrate to microservices structure'"
echo ""
echo -e "${YELLOW}⚠️  Don't forget to:${NC}"
echo "  - Update .github/workflows/ paths"
echo "  - Update docker-compose.yml paths"
echo "  - Test local builds"
echo ""
