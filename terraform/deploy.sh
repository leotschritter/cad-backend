#!/bin/bash
# ===================================================
# CAD Travel App - IaaS Deployment Script
# ===================================================

set -euo pipefail

# Allow overriding project via environment (CI)
PROJECT_ID=${GCP_PROJECT_ID:-graphite-plane-474510-s9}
REGION=${GCP_REGION:-europe-west3}
ZONE=${GCP_ZONE:-europe-west3-a}

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
gcloud config set project "$PROJECT_ID"
echo "✓ Projekt gesetzt: $PROJECT_ID"
echo ""

# Schritt 3: APIs aktivieren
echo "[3/6] Aktiviere benötigte APIs..."
gcloud services enable compute.googleapis.com \
  sqladmin.googleapis.com \
  storage.googleapis.com \
  iam.googleapis.com \
  firestore.googleapis.com \
  servicenetworking.googleapis.com --quiet || true

echo "✓ APIs aktiviert (oder bereits aktiviert)"
echo ""

# Schritt 4: Terraform initialisieren
echo "[4/6] Terraform initialisieren..."
# cd into terraform folder (script assumed to live in terraform/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
terraform init -input=false
echo "✓ Terraform initialisiert"
echo ""

# Schritt 5: Terraform Plan
echo "[5/6] Terraform Plan erstellen..."
terraform plan -out=tfplan -input=false
echo "✓ Plan erstellt"
echo ""

# Schritt 6: Deployment bestätigen / Ausführen
echo "[6/6] Deployment durchführen..."
if [ "${CI:-}" = "true" ]; then
    echo "CI-Modus detected: automatisches Apply"
    terraform apply -input=false -auto-approve tfplan
else
    read -p "Möchtest du das Deployment jetzt starten? (yes/no): " CONFIRM
    if [ "$CONFIRM" = "yes" ]; then
        terraform apply tfplan
    else
        echo "❌ Deployment abgebrochen"
        rm -f tfplan
        exit 0
    fi
fi

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║              Deployment erfolgreich abgeschlossen!             ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Outputs anzeigen (falls vorhanden)
if terraform output -json | jq -e . >/dev/null 2>&1; then
  terraform output
else
  terraform output || true
fi

echo ""
echo "💡 Tipps:"
echo "  - Warte 2-3 Minuten bis die Container gestartet sind"
echo "  - Prüfe Logs mit: ./check-deployment.sh"
echo "  - Aktualisiere Images mit: ./update-images.sh"
