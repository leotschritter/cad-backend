#!/bin/bash
# ===================================================
# CAD Travel App - IaaS Deployment Script
# ===================================================

set -e

PROJECT_ID="graphite-plane-474510-s9"
REGION="europe-west3"
ZONE="europe-west3-a"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║         CAD Travel App - IaaS Deployment Starten               ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Schritt 1: GCloud Auth prüfen
echo "[1/6] Prüfe GCloud Authentifizierung..."
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q .; then
    echo "⚠️  Nicht authentifiziert! Führe aus:"
    echo "    gcloud auth application-default login"
    exit 1
fi
echo "✓ Authentifizierung OK"
echo ""

# Schritt 2: Projekt setzen
echo "[2/6] Setze GCloud Projekt..."
gcloud config set project $PROJECT_ID
echo "✓ Projekt gesetzt: $PROJECT_ID"
echo ""

# Schritt 3: APIs aktivieren
echo "[3/6] Aktiviere benötigte APIs..."
gcloud services enable compute.googleapis.com \
  sqladmin.googleapis.com \
  storage.googleapis.com \
  iam.googleapis.com \
  firestore.googleapis.com \
  servicenetworking.googleapis.com --quiet

echo "✓ APIs aktiviert"
echo ""

# Schritt 4: Terraform initialisieren
echo "[4/6] Terraform initialisieren..."
cd "$(dirname "$0")"
terraform init
echo "✓ Terraform initialisiert"
echo ""

# Schritt 5: Terraform Plan
echo "[5/6] Terraform Plan erstellen..."
terraform plan -out=tfplan
echo "✓ Plan erstellt"
echo ""

# Schritt 6: Deployment bestätigen
echo "[6/6] Deployment durchführen..."
read -p "Möchtest du das Deployment jetzt starten? (yes/no): " CONFIRM

if [ "$CONFIRM" = "yes" ]; then
    terraform apply tfplan

    echo ""
    echo "╔════════════════════════════════════════════════════════════════╗"
    echo "║              Deployment erfolgreich abgeschlossen!             ║"
    echo "╚════════════════════════════════════════════════════════════════╝"
    echo ""

    # Outputs anzeigen
    terraform output deployment_summary

    echo ""
    echo "💡 Tipps:"
    echo "  - Warte 2-3 Minuten bis die Container gestartet sind"
    echo "  - Prüfe Logs mit: ./check-deployment.sh"
    echo "  - Aktualisiere Images mit: ./update-images.sh"

else
    echo "❌ Deployment abgebrochen"
    rm -f tfplan
    exit 0
fi

