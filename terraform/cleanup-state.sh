#!/bin/bash
# ===================================================
# Cleanup existierende Ressourcen aus Terraform State
# Verwende dies nur, wenn Import-Probleme auftreten
# ===================================================

set -e

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     Entferne problematische Ressourcen aus Terraform State    ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

cd "$(dirname "$0")"

echo "⚠️  WARNUNG: Dies entfernt Ressourcen aus dem Terraform State."
echo "   Die Ressourcen in GCP bleiben unverändert."
echo ""
read -p "Möchtest du fortfahren? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
  echo "❌ Abgebrochen"
  exit 0
fi

echo ""
echo "[1/3] Entferne Firestore Indexes aus State..."
terraform state rm google_firestore_index.comments_by_itinerary 2>/dev/null && echo "  ✓ comments_by_itinerary entfernt" || echo "  ↪ Nicht im State"
terraform state rm google_firestore_index.comments_by_user 2>/dev/null && echo "  ✓ comments_by_user entfernt" || echo "  ↪ Nicht im State"
terraform state rm google_firestore_index.likes_by_user 2>/dev/null && echo "  ✓ likes_by_user entfernt" || echo "  ↪ Nicht im State"

echo ""
echo "[2/3] Zeige verbleibende Ressourcen im State..."
terraform state list

echo ""
echo "[3/3] Fertig!"
echo ""
echo "💡 Nächste Schritte:"
echo "  1. Führe ./deploy.sh aus"
echo "  2. Das Script wird die Ressourcen neu importieren"
echo ""

