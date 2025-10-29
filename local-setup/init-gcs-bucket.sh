#!/bin/bash

# Initialize GCS bucket in the local emulator
# This script creates the necessary bucket for image storage

BUCKET_NAME="tripico-images"
GCS_HOST="http://localhost:4443"

echo "Initializing GCS bucket: $BUCKET_NAME"
echo "GCS Emulator Host: $GCS_HOST"

# Wait for GCS emulator to be ready
echo "Waiting for GCS emulator to be ready..."
MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s -f "$GCS_HOST/storage/v1/b" > /dev/null 2>&1; then
        echo "GCS emulator is ready!"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "Waiting for GCS emulator... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "ERROR: GCS emulator did not start in time"
    echo "Please check if the gcs-emulator container is running:"
    echo "  docker-compose ps gcs-emulator"
    exit 1
fi

# Create the bucket
echo "Creating bucket: $BUCKET_NAME"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$GCS_HOST/storage/v1/b" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"$BUCKET_NAME\"}")

HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 409 ]; then
    echo "✓ Bucket '$BUCKET_NAME' is ready!"
    
    # List buckets to verify
    echo ""
    echo "Available buckets:"
    curl -s "$GCS_HOST/storage/v1/b" | grep -o '"name":"[^"]*"' || echo "  - $BUCKET_NAME"
    
    echo ""
    echo "✓ GCS emulator setup complete!"
    echo ""
    echo "You can now upload files to: $GCS_HOST/storage/v1/b/$BUCKET_NAME/o"
else
    echo "✗ Failed to create bucket"
    echo "HTTP Code: $HTTP_CODE"
    echo "Response: $BODY"
    exit 1
fi

