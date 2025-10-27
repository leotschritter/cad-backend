#!/bin/bash

# Start all local development services and initialize them
# This script provides a complete local development environment

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=================================================="
echo "  Travel App - Local Development Environment"
echo "=================================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored messages
print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}!${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker first."
    exit 1
fi
print_success "Docker is running"

# Navigate to local-setup directory
cd "$SCRIPT_DIR"

# Stop any existing containers
echo ""
echo "Stopping existing containers (if any)..."
docker-compose down > /dev/null 2>&1 || true

# Start all services
echo ""
echo "Starting services..."
docker compose up -d

# Wait for services to be healthy
echo ""
echo "Waiting for services to be ready..."

# Wait for PostgreSQL
echo -n "  PostgreSQL... "
MAX_RETRIES=30
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if docker exec postgres_db pg_isready -U myuser > /dev/null 2>&1; then
        print_success "Ready"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    sleep 1
done
if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    print_error "PostgreSQL failed to start"
    exit 1
fi

# Wait for Firestore Emulator
echo -n "  Firestore Emulator... "
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s -f http://localhost:8081 > /dev/null 2>&1; then
        print_success "Ready"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    sleep 1
done
if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    print_error "Firestore Emulator failed to start"
    exit 1
fi

# Wait for GCS Emulator
echo -n "  GCS Emulator... "
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s -f http://localhost:4443/storage/v1/b > /dev/null 2>&1; then
        print_success "Ready"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    sleep 1
done
if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    print_error "GCS Emulator failed to start"
    exit 1
fi

# Initialize GCS bucket
echo ""
echo "Initializing GCS bucket..."
./init-gcs-bucket.sh > /dev/null 2>&1 && print_success "GCS bucket created" || print_warning "GCS bucket might already exist"

# Print service information
echo ""
echo "=================================================="
echo "  Services Ready!"
echo "=================================================="
echo ""
echo "PostgreSQL:"
echo "  Host: localhost:5432"
echo "  Database: mydatabase"
echo "  User: myuser"
echo "  Password: mypassword"
echo ""
echo "Firestore Emulator:"
echo "  Host: localhost:8081"
echo "  Project ID: local-project"
echo ""
echo "GCS Emulator:"
echo "  Host: localhost:4443"
echo "  Bucket: tripico-images"
echo ""
echo "=================================================="
echo ""
echo "To start the application, run:"
echo "  cd $PROJECT_ROOT"
echo "  mvn quarkus:dev -Dquarkus.profile=local"
echo ""
echo "To stop all services, run:"
echo "  cd $SCRIPT_DIR"
echo "  docker compose down"
echo ""
echo "=================================================="

