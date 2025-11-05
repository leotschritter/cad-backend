#!/usr/bin/env bash
set -euo pipefail

# pipeline_deploy_vm.sh
# Remote deploy script intended to be executed from CI (GitHub Actions).
# It SSHes to the target VM, updates DuckDNS (optional), pulls new Docker images
# and restarts docker-compose, and optionally runs certbot.
#
# Required environment variables (set as GitHub Secrets):
# - SSH_USER            e.g. ubuntu
# - SSH_HOST            e.g. 35.234.115.11
# - SSH_PORT            optional, default 22
# - SSH_PRIVATE_KEY     private key text OR
# - SSH_PRIVATE_KEY_BASE64  base64 encoded private key
# - REMOTE_APP_DIR      path on VM where docker-compose.yml lives (default: /opt/cad-travel)
# Optional variables:
# - DUCKDNS_DOMAIN
# - DUCKDNS_TOKEN
# - TARGET_DOMAIN       domain used for certbot (e.g. tripico.duckdns.org)
# - CERTBOT_EMAIL
# - COMPOSE_ENV_FILE    env file name on remote (default: .env)

# ---- Helper / validation ----
require() {
  if [ -z "${!1:-}" ]; then
    echo "Missing required env var: $1" >&2
    exit 2
  fi
}

# SSH key setup
KEY_FILE="/tmp/pipeline_deploy_key_$$"
cleanup() {
  rm -f "$KEY_FILE"
}
trap cleanup EXIT

if [ -n "${SSH_PRIVATE_KEY_BASE64:-}" ]; then
  echo "Decoding SSH key from base64..."
  echo "$SSH_PRIVATE_KEY_BASE64" | base64 --decode > "$KEY_FILE"
elif [ -n "${SSH_PRIVATE_KEY:-}" ]; then
  echo "Writing SSH key..."
  printf '%s\n' "$SSH_PRIVATE_KEY" > "$KEY_FILE"
else
  echo "Either SSH_PRIVATE_KEY or SSH_PRIVATE_KEY_BASE64 must be set" >&2
  exit 2
fi
chmod 600 "$KEY_FILE"

SSH_PORT=${SSH_PORT:-22}
SSH_USER=${SSH_USER:-}
SSH_HOST=${SSH_HOST:-}
REMOTE_APP_DIR=${REMOTE_APP_DIR:-/opt/cad-travel}
COMPOSE_ENV_FILE=${COMPOSE_ENV_FILE:-.env}
TARGET_DOMAIN=${TARGET_DOMAIN:-}
CERTBOT_EMAIL=${CERTBOT_EMAIL:-}
DUCKDNS_DOMAIN=${DUCKDNS_DOMAIN:-}
DUCKDNS_TOKEN=${DUCKDNS_TOKEN:-}

require SSH_USER
require SSH_HOST

SSH_OPTS=(-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i "$KEY_FILE" -p "$SSH_PORT")

echo "[CI] Deploying to ${SSH_USER}@${SSH_HOST}:${SSH_PORT} (remote dir: ${REMOTE_APP_DIR})"

# The remote script will be run via ssh and will receive the environment variables
# DUCKDNS_DOMAIN, DUCKDNS_TOKEN, TARGET_DOMAIN, CERTBOT_EMAIL and REMOTE_APP_DIR

ssh ${SSH_OPTS[@]} "${SSH_USER}@${SSH_HOST}" "DUCKDNS_DOMAIN='${DUCKDNS_DOMAIN}' DUCKDNS_TOKEN='${DUCKDNS_TOKEN}' TARGET_DOMAIN='${TARGET_DOMAIN}' CERTBOT_EMAIL='${CERTBOT_EMAIL}' REMOTE_APP_DIR='${REMOTE_APP_DIR}' COMPOSE_ENV_FILE='${COMPOSE_ENV_FILE}' bash -s" <<'REMOTE_EOF'
set -euo pipefail

echo "[REMOTE] Starting remote deploy script"

echo "[REMOTE] Working dir: ${REMOTE_APP_DIR}"
mkdir -p "${REMOTE_APP_DIR}"
cd "${REMOTE_APP_DIR}"

# Optional DuckDNS update
if [ -n "${DUCKDNS_DOMAIN}" ] && [ -n "${DUCKDNS_TOKEN}" ]; then
  echo "[REMOTE] Updating DuckDNS: ${DUCKDNS_DOMAIN}"
  resp=$(curl -s "https://www.duckdns.org/update?domains=${DUCKDNS_DOMAIN}&token=${DUCKDNS_TOKEN}&ip=")
  echo "[REMOTE] DuckDNS response: ${resp}"
  if [ "${resp}" = "OK" ]; then
    echo "[REMOTE] Waiting for DNS to propagate for ${DUCKDNS_DOMAIN} (max 60s)..."
    for i in $(seq 1 30); do
      if command -v dig >/dev/null 2>&1; then
        resolved=$(dig +short ${DUCKDNS_DOMAIN} | head -n1 || true)
      else
        resolved=$(getent ahosts ${DUCKDNS_DOMAIN} | awk '{print $1; exit}' || true)
      fi
      if [ -n "${resolved}" ]; then
        echo "[REMOTE] ${DUCKDNS_DOMAIN} resolves to ${resolved}"
        break
      fi
      sleep 2
    done
  else
    echo "[REMOTE] DuckDNS update failed or returned non-OK: ${resp}"
  fi
else
  echo "[REMOTE] No DuckDNS credentials provided — skipping"
fi

# Pull images and restart containers
echo "[REMOTE] Pulling images and (re)starting docker-compose"
docker-compose --env-file "${COMPOSE_ENV_FILE}" pull
# Use up -d to recreate if image changed
docker-compose --env-file "${COMPOSE_ENV_FILE}" up -d

# Optional: install/renew cert with certbot
if [ -n "${TARGET_DOMAIN}" ]; then
  echo "[REMOTE] Running certbot for ${TARGET_DOMAIN}"
  certbot --nginx -d "${TARGET_DOMAIN}" --non-interactive --agree-tos --email "${CERTBOT_EMAIL:-no-reply@example.com}" --redirect || echo "[REMOTE] certbot failed (check DNS and nginx)"
else
  echo "[REMOTE] No TARGET_DOMAIN provided — skipping certbot"
fi

# Show status
echo "[REMOTE] docker-compose ps:"
docker-compose ps

echo "[REMOTE] Remote deploy finished"
REMOTE_EOF

rc=$?
if [ $rc -ne 0 ]; then
  echo "[CI] Remote deploy returned non-zero exit code: $rc" >&2
  exit $rc
fi

echo "[CI] Deploy completed successfully"

# cleanup is automatic via trap

exit 0
# 🔥 Firebase Config - Wie bekomme ich die Werte?

## ⚠️ Wichtig: Reihenfolge!

Die Firebase Config-Werte (API Key, Auth Domain, etc.) existieren **ERST NACH** dem Terraform Deployment!

## 📋 Ablauf

### 1️⃣ Terraform Apply (erstellt Firebase)
```bash
cd terraform
terraform apply
```

### 2️⃣ Config auslesen
```bash
# Direkt anzeigen
./get-firebase-config.sh

# Als Datei speichern
./get-firebase-config.sh > firebase-config.txt

# Nur bestimmtes Format
terraform output firebase_config
```

## 🎯 Drei Wege zur Integration

### Option A: Manuell ins Frontend kopieren ✋

**Nach Terraform Apply:**
```bash
cd terraform
./get-firebase-config.sh
```

Kopiere die Werte dann in:
- `frontend/.env.local` (lokal)
- `frontend/src/firebase/config.ts` (direkt im Code)

**Beispiel `.env.local`:**
```env
VITE_FIREBASE_API_KEY=AIzaSy...
VITE_FIREBASE_AUTH_DOMAIN=iaas-476910.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=iaas-476910
VITE_FIREBASE_STORAGE_BUCKET=iaas-476910-travel-images
VITE_FIREBASE_APP_ID=1:1234567890:web:abc123
```

### Option B: GitHub Actions Secrets 🤖

**Nach Terraform Apply:**
```bash
cd terraform
./get-firebase-config.sh
```

Kopiere die Werte als GitHub Secrets:
1. GitHub Repo → Settings → Secrets and variables → Actions
2. Füge hinzu:
   - `FIREBASE_API_KEY`
   - `FIREBASE_AUTH_DOMAIN`
   - `FIREBASE_PROJECT_ID`
   - `FIREBASE_STORAGE_BUCKET`
   - `FIREBASE_APP_ID`

**In Frontend-Pipeline verwenden:**
```yaml
- name: Build Frontend
  env:
    VITE_FIREBASE_API_KEY: ${{ secrets.FIREBASE_API_KEY }}
    VITE_FIREBASE_AUTH_DOMAIN: ${{ secrets.FIREBASE_AUTH_DOMAIN }}
    VITE_FIREBASE_PROJECT_ID: ${{ secrets.FIREBASE_PROJECT_ID }}
    VITE_FIREBASE_STORAGE_BUCKET: ${{ secrets.FIREBASE_STORAGE_BUCKET }}
    VITE_FIREBASE_APP_ID: ${{ secrets.FIREBASE_APP_ID }}
  run: npm run build
```

### Option C: Automatisch via CI (deploy-iaas.yml) 🚀

Die `deploy-iaas.yml` Pipeline extrahiert Firebase Config automatisch!

**Nach Pipeline-Run:**
1. Gehe zu: Actions → Deploy to GCP → letzter Run
2. Scrolle nach unten → "Summary"
3. Dort siehst du die Firebase Config!

**Oder nutze Outputs in weiteren Steps:**
```yaml
- name: Build Frontend with Firebase Config
  env:
    VITE_FIREBASE_API_KEY: ${{ steps.firebase.outputs.api_key }}
    VITE_FIREBASE_AUTH_DOMAIN: ${{ steps.firebase.outputs.auth_domain }}
  run: npm run build
```

## 📝 Kompletter Workflow

### Szenario 1: Erstes Deployment

```bash
# 1. Terraform Apply (erstellt alles inkl. Firebase)
cd terraform
terraform apply

# 2. Firebase Config auslesen
./get-firebase-config.sh

# 3a. Lokal entwickeln: in .env.local kopieren
./get-firebase-config.sh > ../frontend/.env.local

# 3b. Für Produktion: als GitHub Secrets speichern
# (manuell in GitHub UI eintragen)

# 4. Frontend neu bauen mit Config
cd ../frontend
npm run build

# 5. Docker Image bauen & pushen
docker build -t ghcr.io/leotschritter/cad-frontend:iaas-latest .
docker push ghcr.io/leotschritter/cad-frontend:iaas-latest

# 6. VM Images updaten
gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a \
  --command='cd /opt/cad-travel && docker-compose pull && docker-compose up -d'
```

### Szenario 2: Nur Config aktualisiert

Wenn du z.B. `authorized_domains` änderst:

```bash
# 1. Terraform Apply
cd terraform
terraform apply

# 2. Config neu auslesen (sollte gleich bleiben, außer domains)
./get-firebase-config.sh

# 3. Nur wenn API Key sich ändert: Frontend neu bauen
# (normalerweise ändert sich nur authorized_domains, nicht die Keys)
```

### Szenario 3: CI/CD via GitHub Actions

```bash
# 1. Push Code oder manuell triggern
git push origin main
# oder: GitHub → Actions → Deploy to GCP → Run workflow

# 2. Warte auf Pipeline-Ende

# 3. Config aus Actions Summary kopieren
# GitHub → Actions → letzter Run → Summary → Firebase Config

# 4. In Frontend-Repo als Secrets speichern (einmalig)

# 5. Frontend-Pipeline triggern (nutzt die Secrets)
```

## 🔍 Config prüfen

### Ist Firebase deployed?
```bash
cd terraform
terraform output firebase_config
```

**Erwartete Ausgabe:**
```json
{
  "apiKey" = "AIzaSy..."
  "authDomain" = "iaas-476910.firebaseapp.com"
  "projectId" = "iaas-476910"
  "storageBucket" = "iaas-476910-travel-images"
  "appId" = "1:123456789:web:abc123"
}
```

**Falls leer oder Error:**
- Firebase wurde noch nicht deployed
- Führe `terraform apply` aus

### Funktioniert die Config?
```bash
# Console öffnen
open "https://console.firebase.google.com/project/iaas-476910/authentication"

# Prüfe ob "Authentication" aktiviert ist
```

## ⚡ Schnellbefehle

```bash
# Config anzeigen
cd terraform && ./get-firebase-config.sh

# In Frontend .env speichern
cd terraform && ./get-firebase-config.sh | grep "^VITE_" > ../frontend/.env.local

# Nur TypeScript-Format
cd terraform && ./get-firebase-config.sh | sed -n '/const firebaseConfig/,/};/p'

# GitHub Secrets Format
cd terraform && ./get-firebase-config.sh | grep "^FIREBASE_"

# Deployment + Config in einem Schritt
cd terraform && ./deploy.sh && ./get-firebase-config.sh
```

## 🆘 Troubleshooting

### "Firebase Config nicht gefunden"
**Problem:** `terraform output firebase_config` gibt nichts zurück

**Lösung:**
```bash
# Prüfe State
terraform state list | grep firebase

# Falls leer: Firebase noch nicht deployed
terraform apply

# Falls vorhanden aber Output fehlt: outputs.tf prüfen
grep firebase_config outputs.tf
```

### "Config leer oder null"
**Problem:** Config existiert aber Werte sind leer

**Lösung:**
```bash
# Terraform State neu laden
terraform refresh

# Output erneut versuchen
terraform output firebase_config

# Notfall: Console aufrufen
open "https://console.firebase.google.com/project/iaas-476910/settings/general"
# Web-App auswählen → Config manuell kopieren
```

### "API Key funktioniert nicht im Frontend"
**Problem:** Firebase Init schlägt fehl

**Checkliste:**
- [ ] Ist `firebase` npm package installiert? (`npm install firebase`)
- [ ] Sind die Env-Variablen gesetzt? (`console.log(import.meta.env.VITE_FIREBASE_API_KEY)`)
- [ ] Ist die Domain in `authorized_domains`? (Terraform oder Console prüfen)
- [ ] Browser-Console auf Fehler prüfen

## 📚 Weiterführend

- `FIREBASE_QUICKSTART.md` - Firebase Setup
- `FIREBASE_AUTH_SETUP.md` - Vollständige Doku
- `FIREBASE_CHECKLIST.md` - Schritt-für-Schritt

---

**Wichtig:** Die Firebase Config-Werte (besonders API Key) sind **public** und dürfen ins Frontend. Sie werden durch `authorized_domains` geschützt!

