#!/bin/bash
# ===================================================
# Post-Deployment Script
# Executes startup script on the deployed VM
# ===================================================

set -euo pipefail

# Get configuration from terraform or environment
PROJECT_ID=${GCP_PROJECT_ID:-$(terraform output -raw project_id 2>/dev/null || echo "iaas-476910")}
ZONE=${GCP_ZONE:-$(terraform output -raw zone 2>/dev/null || echo "europe-west3-a")}
VM_NAME=${VM_NAME:-"cad-travel-app-vm"}

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║         Post-Deployment: Executing Startup Script              ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check if VM exists and is running
echo "[1/4] Checking VM status..."
VM_STATUS=$(gcloud compute instances describe "$VM_NAME" \
  --zone="$ZONE" \
  --project="$PROJECT_ID" \
  --format="value(status)" 2>/dev/null || echo "NOT_FOUND")

if [ "$VM_STATUS" = "NOT_FOUND" ]; then
  echo "❌ Error: VM '$VM_NAME' not found in project '$PROJECT_ID'"
  exit 1
elif [ "$VM_STATUS" != "RUNNING" ]; then
  echo "⚠️  VM status: $VM_STATUS"
  echo "   Waiting for VM to start..."
  gcloud compute instances start "$VM_NAME" --zone="$ZONE" --project="$PROJECT_ID" 2>/dev/null || true
  sleep 10
fi

echo "✓ VM is running"
echo ""

# Wait for SSH to be available
echo "[2/4] Waiting for SSH to be available..."
MAX_RETRIES=12
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
  if gcloud compute ssh "x" \
    --zone="$ZONE" \
    --project="$PROJECT_ID" \
    --command="echo 'SSH ready'" \
    --ssh-flag="-o ConnectTimeout=5" \
    --quiet 2>/dev/null; then
    echo "✓ SSH connection established"
    break
  fi

  RETRY_COUNT=$((RETRY_COUNT + 1))
  if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "❌ Error: Could not establish SSH connection after $MAX_RETRIES attempts"
    exit 1
  fi

  echo "   Attempt $RETRY_COUNT/$MAX_RETRIES - waiting 5 seconds..."
  sleep 5
done

echo ""

# Execute the startup script on the VM
echo "[3/4] Executing startup script on VM..."
echo "   This may take 5-10 minutes (installing packages, pulling images, etc.)"
echo ""

# The startup script is already in the VM metadata, so we execute it
gcloud compute ssh "$VM_NAME" \
  --zone="$ZONE" \
  --project="$PROJECT_ID" \
  --command="sudo bash /var/lib/google/scripts/startup-script" 2>&1 | tee /tmp/startup-execution.log

if [ ${PIPESTATUS[0]} -ne 0 ]; then
  echo ""
  echo "❌ Startup script execution failed!"
  echo "   Check logs with:"
  echo "   gcloud compute ssh $VM_NAME --zone=$ZONE --project=$PROJECT_ID --command='sudo cat /var/log/startup-script.log'"
  exit 1
fi

echo ""
echo "✓ Startup script executed successfully"
echo ""

# Verify deployment
echo "[4/4] Verifying deployment..."

# Wait a bit for containers to start
sleep 15

# Check if docker-compose is running
DOCKER_STATUS=$(gcloud compute ssh "$VM_NAME" \
  --zone="$ZONE" \
  --project="$PROJECT_ID" \
  --command="cd /opt/cad-travel && sudo docker-compose ps --format json 2>/dev/null || echo '[]'" 2>/dev/null)

if [ "$DOCKER_STATUS" = "[]" ] || [ -z "$DOCKER_STATUS" ]; then
  echo "⚠️  Warning: Containers not yet started or docker-compose not found"
  echo "   This might be normal if this is the first deployment"
else
  echo "✓ Docker containers detected"
  gcloud compute ssh "$VM_NAME" \
    --zone="$ZONE" \
    --project="$PROJECT_ID" \
    --command="cd /opt/cad-travel && sudo docker-compose ps" 2>/dev/null || true
fi

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║         Post-Deployment Completed Successfully!                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "🌐 Application URLs:"
echo "   Frontend: http://$(terraform output -raw vm_external_ip 2>/dev/null || echo '<VM_IP>'):5173"
echo "   Backend:  http://$(terraform output -raw vm_external_ip 2>/dev/null || echo '<VM_IP>'):8080"
echo "   API Docs: http://$(terraform output -raw vm_external_ip 2>/dev/null || echo '<VM_IP>'):8080/q/swagger-ui"
echo ""
echo "📋 Useful commands:"
echo "   View logs:      gcloud compute ssh $VM_NAME --zone=$ZONE --project=$PROJECT_ID --command='sudo cat /var/log/startup-script.log'"
echo "   Container logs: gcloud compute ssh $VM_NAME --zone=$ZONE --project=$PROJECT_ID --command='cd /opt/cad-travel && sudo docker-compose logs -f'"
echo "   SSH to VM:      gcloud compute ssh $VM_NAME --zone=$ZONE --project=$PROJECT_ID"
echo ""

