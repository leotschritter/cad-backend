#!/bin/bash
# ===================================================
# CAD Travel App - Docker Images aktualisieren
# ===================================================

set -e

ZONE="europe-west3-a"
PROJECT_ID="graphite-plane-474510-s9"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║           CAD Travel App - Images aktualisieren                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

echo "🔄 Pulling neueste Images und Neustart der Container..."
echo ""

gcloud compute ssh cad-travel-app-vm \
  --zone=$ZONE \
  --project=$PROJECT_ID \
  --command='cd /opt/cad-travel && sudo docker-compose pull && sudo docker-compose up -d --force-recreate'

echo ""
echo "⏳ Warte 10 Sekunden..."
sleep 10

echo ""
echo "✓ Update abgeschlossen!"
echo ""

echo "📋 Container Status:"
gcloud compute ssh cad-travel-app-vm \
  --zone=$ZONE \
  --project=$PROJECT_ID \
  --command='cd /opt/cad-travel && sudo docker-compose ps'

echo ""
echo "💡 Logs anzeigen mit:"
echo "   ./check-deployment.sh"

