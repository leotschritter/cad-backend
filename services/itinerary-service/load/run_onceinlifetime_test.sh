#!/bin/bash
# Script to run the once-in-a-lifetime workload test

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================="
echo "🔥 Once-in-a-Lifetime Workload Load Test"
echo "========================================="
echo ""

# Check if .env.onceinlifetime exists
if [ ! -f ".env.onceinlifetime" ]; then
    echo "⚠️  .env.onceinlifetime not found!"
    echo "Creating from config_onceinlifetime.txt..."
    cp config_onceinlifetime.txt .env.onceinlifetime
    echo "✅ Created .env.onceinlifetime - please review and adjust if needed"
    echo ""
fi

# Load environment variables
source .env.onceinlifetime

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "📦 Creating virtual environment..."
    python3 -m venv venv
    echo "✅ Virtual environment created"
    echo ""
fi

# Activate virtual environment
echo "🔧 Activating virtual environment..."
source venv/bin/activate

# Install/update dependencies
echo "📦 Installing dependencies..."
pip install -q --upgrade pip
pip install -q -r requirements.txt
echo "✅ Dependencies installed"
echo ""

# Create reports directory if it doesn't exist
mkdir -p reports

# Generate report filename
REPORT_NAME="onceinlifetime_test_$(date +%Y%m%d_%H%M%S).html"
REPORT_PATH="reports/$REPORT_NAME"

# Check if we should run in headless mode or web UI
if [ "$1" == "--headless" ] || [ "$1" == "-h" ]; then
    echo "🚀 Running test in headless mode..."
    echo ""
    echo "Configuration:"
    echo "  Host: ${LOCUST_HOST}"
    echo "  Users: ${USERS}"
    echo "  Spawn Rate: ${SPAWN_RATE}"
    echo "  Duration: ${RUN_TIME}"
    echo "  Report: ${REPORT_PATH}"
    echo ""
    
    locust -f locustfile_onceinlifetime.py \
        --host "${LOCUST_HOST}" \
        --users "${USERS}" \
        --spawn-rate "${SPAWN_RATE}" \
        --run-time "${RUN_TIME}" \
        --html "${REPORT_PATH}" \
        --headless
    
    echo ""
    echo "========================================="
    echo "✅ Test completed!"
    echo "========================================="
    echo ""
    echo "📊 Report saved to: ${REPORT_PATH}"
    echo "Open the report in your browser to view results."
    echo ""
else
    echo "🌐 Starting Locust Web UI..."
    echo ""
    echo "Configuration:"
    echo "  Host: ${LOCUST_HOST}"
    echo "  Suggested Users: ${USERS}"
    echo "  Suggested Spawn Rate: ${SPAWN_RATE}"
    echo "  Suggested Duration: ${RUN_TIME}"
    echo ""
    echo "⚠️  Note: This test simulates a traffic spike!"
    echo "    Higher user count and faster spawn rate than periodic test."
    echo ""
    echo "========================================="
    echo "Next steps:"
    echo "1. Open http://localhost:8089 in your browser"
    echo "2. Enter the test parameters"
    echo "3. Click 'Start swarming'"
    echo "4. Watch for performance degradation"
    echo "5. Download the report when finished"
    echo "========================================="
    echo ""
    
    locust -f locustfile_onceinlifetime.py \
        --host "${LOCUST_HOST}"
fi

