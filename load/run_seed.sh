#!/bin/bash
# Script to run the data seeding process

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================="
echo "🌱 Data Seeding Script"
echo "========================================="
echo ""

# Check if .env.seed exists
if [ ! -f ".env.seed" ]; then
    echo "⚠️  .env.seed not found!"
    echo "Creating from config_seed.txt..."
    cp config_seed.txt .env.seed
    echo "✅ Created .env.seed - please review and adjust if needed"
    echo ""
fi

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

# Run the seed script
echo "🚀 Running data seeding script..."
echo ""
python seed_data.py

echo ""
echo "========================================="
echo "✅ Seeding script completed!"
echo "========================================="
echo ""
echo "Next steps:"
echo "1. Review the output above for any errors"
echo "2. Note the timestamp for use in load tests"
echo "3. Run load tests:"
echo "   - ./run_periodic_test.sh (normal traffic)"
echo "   - ./run_onceinlifetime_test.sh (traffic spike)"
echo ""

