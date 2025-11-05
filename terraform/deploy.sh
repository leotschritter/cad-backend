#!/bin/bash
# ===================================================
# CAD Travel App - IaaS Deployment Script
# ===================================================

set -euo pipefail

# Allow overriding project via environment (CI)
PROJECT_ID=${GCP_PROJECT_ID:-iaas-476910}
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
  servicenetworking.googleapis.com \
  firebase.googleapis.com \
  identitytoolkit.googleapis.com \
  firebasehosting.googleapis.com --quiet || true

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

# Schritt 4.5: Importiere existierende Ressourcen
echo "[4.5/6] Importiere existierende Ressourcen (falls vorhanden)..."

# Prüfe ob Ressourcen bereits im State sind
if ! terraform state show google_compute_instance.app_vm &>/dev/null; then
  echo "  → Importiere VM Instanz..."
  if terraform import google_compute_instance.app_vm \
    "projects/${PROJECT_ID}/zones/${ZONE}/instances/cad-travel-app-vm" 2>&1 | grep -q "Import successful"; then
    echo "  ✓ VM erfolgreich importiert"
  else
    echo "  ↪ VM existiert nicht in GCP, wird neu erstellt"
  fi
else
  echo "  ✓ VM bereits im State"
fi

if ! terraform state show google_firestore_database.database &>/dev/null; then
  echo "  → Importiere Firestore Database..."
  if terraform import google_firestore_database.database \
    "projects/${PROJECT_ID}/databases/(default)" 2>&1 | grep -q "Import successful"; then
    echo "  ✓ Firestore DB erfolgreich importiert"
  else
    echo "  ↪ Firestore DB existiert nicht in GCP, wird neu erstellt"
  fi
else
  echo "  ✓ Firestore DB bereits im State"
fi

# Firestore Indexes - Importiere falls vorhanden
echo "  → Prüfe Firestore Indexes..."

# Versuche jeden Index einzeln zu importieren
# Index 1: comments_by_itinerary
if ! terraform state show google_firestore_index.comments_by_itinerary &>/dev/null; then
  echo "  → Suche comments_by_itinerary Index..."
  COMMENTS_INDEX=$(gcloud firestore indexes composite list \
    --project="${PROJECT_ID}" \
    --filter="collectionGroup:comments" \
    --format="value(name)" 2>/dev/null | head -n1)

  if [ -n "$COMMENTS_INDEX" ]; then
    echo "  → Importiere comments_by_itinerary Index: $COMMENTS_INDEX"
    if terraform import google_firestore_index.comments_by_itinerary "$COMMENTS_INDEX" 2>&1; then
      echo "  ✓ comments_by_itinerary Index importiert"
    else
      echo "  ⚠️  Import fehlgeschlagen, Index wird übersprungen"
    fi
  else
    echo "  ↪ comments_by_itinerary Index nicht gefunden, wird neu erstellt"
  fi
else
  echo "  ✓ comments_by_itinerary Index bereits im State"
fi

# Index 2: likes_by_user
if ! terraform state show google_firestore_index.likes_by_user &>/dev/null; then
  echo "  → Suche likes_by_user Index..."
  LIKES_INDEX=$(gcloud firestore indexes composite list \
    --project="${PROJECT_ID}" \
    --filter="collectionGroup:likes" \
    --format="value(name)" 2>/dev/null | head -n1)

  if [ -n "$LIKES_INDEX" ]; then
    echo "  → Importiere likes_by_user Index: $LIKES_INDEX"
    if terraform import google_firestore_index.likes_by_user "$LIKES_INDEX" 2>&1; then
      echo "  ✓ likes_by_user Index importiert"
    else
      echo "  ⚠️  Import fehlgeschlagen, Index wird übersprungen"
    fi
  else
    echo "  ↪ likes_by_user Index nicht gefunden, wird neu erstellt"
  fi
else
  echo "  ✓ likes_by_user Index bereits im State"
fi

echo "✓ Import abgeschlossen"
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

