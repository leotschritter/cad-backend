#!/bin/bash
# Complete Automated Load Test Script
# Runs against any backend (local or GCloud) with zero manual setup

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "========================================="
echo "🚀 Complete Automated Load Test"
echo "========================================="
echo ""

# Parse command line arguments
BACKEND_URL=""
FIREBASE_API_KEY="./"
NUM_USERS=50
SKIP_SEED=false
TEST_TYPE="both"  # both, periodic, onceinlifetime

while [[ $# -gt 0 ]]; do
    case $1 in
        --backend-url)
            BACKEND_URL="$2"
            shift 2
            ;;
        --firebase-api-key)
            FIREBASE_API_KEY="$2"
            shift 2
            ;;
        --num-users)
            NUM_USERS="$2"
            shift 2
            ;;
        --skip-seed)
            SKIP_SEED=true
            shift
            ;;
        --test-type)
            TEST_TYPE="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --backend-url URL          Backend URL (required)"
            echo "  --firebase-api-key KEY     Firebase API key (required for auth)"
            echo "  --num-users N              Number of test users to create (default: 50)"
            echo "  --skip-seed                Skip data seeding (use existing data)"
            echo "  --test-type TYPE           Test type: periodic, onceinlifetime, or both (default: both)"
            echo "  --help                     Show this help message"
            echo ""
            echo "Example:"
            echo "  $0 --backend-url https://your-app.run.app --firebase-api-key AIzaSy..."
            echo ""
            echo "Example (no auth):"
            echo "  $0 --backend-url http://localhost:8080"
            exit 0
            ;;
        *)
            echo -e "${RED}❌ Unknown option: $1${NC}"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Validate required arguments
if [ -z "$BACKEND_URL" ]; then
    echo -e "${RED}❌ Error: --backend-url is required${NC}"
    echo "Use --help for usage information"
    exit 1
fi

# Determine if we're using auth
USE_AUTH=false
if [ -n "$FIREBASE_API_KEY" ]; then
    USE_AUTH=true
fi

echo -e "${BLUE}Configuration:${NC}"
echo "  Backend URL: $BACKEND_URL"
echo "  Firebase Auth: $([ "$USE_AUTH" = true ] && echo "Enabled" || echo "Disabled")"
echo "  Number of Users: $NUM_USERS"
echo "  Skip Seeding: $SKIP_SEED"
echo "  Test Type: $TEST_TYPE"
echo ""

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo -e "${YELLOW}📦 Creating virtual environment...${NC}"
    python3 -m venv venv
    echo -e "${GREEN}✅ Virtual environment created${NC}"
    echo ""
fi

# Activate virtual environment
echo -e "${BLUE}🔧 Activating virtual environment...${NC}"
source venv/bin/activate

# Install/update dependencies
echo -e "${YELLOW}📦 Installing dependencies...${NC}"
pip install -q --upgrade pip
pip install -q -r requirements.txt
echo -e "${GREEN}✅ Dependencies installed${NC}"
echo ""

# Create .env files automatically
echo -e "${BLUE}📝 Creating configuration files...${NC}"

# Create .env.seed
cat > .env.seed << EOF
LOCUST_HOST=$BACKEND_URL
NUM_USERS=$NUM_USERS
NUM_ITINERARIES_PER_USER=3
NUM_LOCATIONS_PER_ITINERARY=2
CREATE_FIREBASE_USERS=$USE_AUTH
FIREBASE_PASSWORD=LoadTest123!
FIREBASE_API_KEY=$FIREBASE_API_KEY
EOF

# Create .env.periodic
cat > .env.periodic << EOF
LOCUST_HOST=$BACKEND_URL
ORIGIN=http://localhost:5173
FIREBASE_API_KEY=$FIREBASE_API_KEY
USERS=20
SPAWN_RATE=2
RUN_TIME=10m
EOF

# Create .env.onceinlifetime
cat > .env.onceinlifetime << EOF
LOCUST_HOST=$BACKEND_URL
ORIGIN=http://localhost:5173
FIREBASE_API_KEY=$FIREBASE_API_KEY
USERS=100
SPAWN_RATE=10
RUN_TIME=5m
EOF

echo -e "${GREEN}✅ Configuration files created${NC}"
echo ""

# Seed data if not skipping
if [ "$SKIP_SEED" = false ]; then
    echo "========================================="
    echo -e "${BLUE}🌱 Seeding Data${NC}"
    echo "========================================="
    echo ""
    
    python seed_data.py
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✅ Data seeding completed successfully${NC}"
        echo ""
    else
        echo ""
        echo -e "${RED}❌ Data seeding failed${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}⏭️  Skipping data seeding (using existing data)${NC}"
    echo ""
fi

# Create reports directory
mkdir -p reports

# Run periodic workload test
if [ "$TEST_TYPE" = "periodic" ] || [ "$TEST_TYPE" = "both" ]; then
    echo "========================================="
    echo -e "${BLUE}🔄 Running Periodic Workload Test${NC}"
    echo "========================================="
    echo ""
    
    PERIODIC_REPORT="reports/periodic_test_$(date +%Y%m%d_%H%M%S).html"
    
    locust -f locustfile_periodic.py \
        --host "$BACKEND_URL" \
        --users 20 \
        --spawn-rate 2 \
        --run-time 10m \
        --html "$PERIODIC_REPORT" \
        --headless
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✅ Periodic workload test completed${NC}"
        echo -e "${GREEN}📊 Report: $PERIODIC_REPORT${NC}"
        echo ""
    else
        echo ""
        echo -e "${RED}❌ Periodic workload test failed${NC}"
        exit 1
    fi
fi

# Run once-in-a-lifetime workload test
if [ "$TEST_TYPE" = "onceinlifetime" ] || [ "$TEST_TYPE" = "both" ]; then
    echo "========================================="
    echo -e "${BLUE}🔥 Running Once-in-a-Lifetime Workload Test${NC}"
    echo "========================================="
    echo ""
    
    SPIKE_REPORT="reports/onceinlifetime_test_$(date +%Y%m%d_%H%M%S).html"
    
    locust -f locustfile_onceinlifetime.py \
        --host "$BACKEND_URL" \
        --users 100 \
        --spawn-rate 10 \
        --run-time 5m \
        --html "$SPIKE_REPORT" \
        --headless
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✅ Once-in-a-lifetime workload test completed${NC}"
        echo -e "${GREEN}📊 Report: $SPIKE_REPORT${NC}"
        echo ""
    else
        echo ""
        echo -e "${RED}❌ Once-in-a-lifetime workload test failed${NC}"
        exit 1
    fi
fi

echo "========================================="
echo -e "${GREEN}✅ All Tests Completed Successfully!${NC}"
echo "========================================="
echo ""
echo -e "${BLUE}📊 Reports:${NC}"
[ -n "$PERIODIC_REPORT" ] && echo "  Periodic: $PERIODIC_REPORT"
[ -n "$SPIKE_REPORT" ] && echo "  Spike: $SPIKE_REPORT"
echo ""
echo -e "${BLUE}📁 Generated Files:${NC}"
[ -f "test_users.json" ] && echo "  test_users.json (user credentials)"
echo "  .env.seed, .env.periodic, .env.onceinlifetime"
echo ""
echo -e "${GREEN}🎉 Load testing complete!${NC}"
echo ""



