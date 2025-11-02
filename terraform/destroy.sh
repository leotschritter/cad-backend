#!/bin/bash
# ===================================================
# CAD Travel App - Deployment zerstören
# ===================================================

set -e

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║        CAD Travel App - Deployment löschen                     ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

cd "$(dirname "$0")"

echo "⚠️  WARNUNG: Dies wird ALLE Ressourcen löschen:"
echo "  - VM Instanz"
echo "  - Cloud SQL Datenbank"
echo "  - Storage Bucket (inkl. aller Bilder)"
echo "  - Service Accounts"
echo "  - Firewall Regeln"
echo ""

read -p "Bist du sicher? Gib 'DELETE' ein zum Bestätigen: " CONFIRM

if [ "$CONFIRM" = "DELETE" ]; then
    echo ""
    echo "🗑️  Lösche Ressourcen..."
    terraform destroy

    echo ""
    echo "✓ Alle Ressourcen wurden gelöscht"
    echo ""

    # Cleanup
    rm -f tfplan terraform.tfstate.backup

else
    echo "❌ Abgebrochen - nichts wurde gelöscht"
fi
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
#!/bin/bash
# ===================================================
# CAD Travel App - Deployment Status prüfen
# ===================================================

set -e

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║           CAD Travel App - Deployment Status                   ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

cd "$(dirname "$0")"

# VM Name und Zone aus Terraform holen
VM_NAME=$(terraform output -raw vm_external_ip 2>/dev/null || echo "")
ZONE="europe-west3-a"
PROJECT_ID="graphite-plane-474510-s9"

if [ -z "$VM_NAME" ]; then
    echo "❌ Terraform Output nicht verfügbar. Ist das Deployment abgeschlossen?"
    exit 1
fi

echo "🖥️  VM Status:"
echo "─────────────────────────────────────────────────────────────────"
gcloud compute instances describe cad-travel-app-vm \
  --zone=$ZONE \
  --project=$PROJECT_ID \
  --format="table(name,status,networkInterfaces[0].accessConfigs[0].natIP)"

echo ""
echo "🐳 Container Status:"
echo "─────────────────────────────────────────────────────────────────"
gcloud compute ssh cad-travel-app-vm \
  --zone=$ZONE \
  --project=$PROJECT_ID \
  --command='cd /opt/cad-travel && sudo docker-compose ps'

echo ""
echo "📋 Letzte 20 Log-Zeilen:"
echo "─────────────────────────────────────────────────────────────────"
gcloud compute ssh cad-travel-app-vm \
  --zone=$ZONE \
  --project=$PROJECT_ID \
  --command='cd /opt/cad-travel && sudo docker-compose logs --tail=20'

echo ""
echo "🌐 URLs:"
echo "─────────────────────────────────────────────────────────────────"
terraform output frontend_url
terraform output backend_url
terraform output backend_api_docs

echo ""
echo "💡 Vollständige Logs anzeigen:"
echo "   gcloud compute ssh cad-travel-app-vm --zone=$ZONE --command='cd /opt/cad-travel && sudo docker-compose logs -f'"

