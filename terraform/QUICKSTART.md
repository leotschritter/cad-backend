# 🚀 IaaS Deployment - Quick Start

## ✅ Voraussetzungen

- Google Cloud Project: `graphite-plane-474510-s9`
- gcloud CLI installiert
- Terraform >= 1.5.0

---

## 📋 3-Schritte-Deployment

### 1️⃣ Konfiguration

Erstelle `terraform.tfvars`:

```bash
cd terraform/
cat > terraform.tfvars << 'EOF'
project_id = "graphite-plane-474510-s9"
region     = "europe-west3"
zone       = "europe-west3-a"
EOF
```

### 2️⃣ Deployment

```bash
./deploy.sh
```

Das Script:
- ✅ Prüft GCloud Auth
- ✅ Aktiviert APIs
- ✅ Initialisiert Terraform
- ✅ Erstellt Plan
- ✅ Deployed Infrastruktur
- ✅ **Führt automatisch Post-Deploy aus** (installiert Docker, startet Container)

**Hinweis:** Das Deployment dauert ~10-15 Minuten.

#### Optional: Post-Deploy überspringen
```bash
SKIP_POST_DEPLOY=true ./deploy.sh
# Später manuell ausführen:
./post-deploy.sh
```

#### Manuelles Post-Deploy (falls nötig)
```bash
# Linux/Mac/Windows (Git Bash/WSL)
./post-deploy.sh
```

#### SSL Setup (nach Deployment)
```bash
# SSH zur VM
gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a

# SSL manuell installieren
sudo certbot --nginx \
    -d tripico.duckdns.org \
    --non-interactive \
    --agree-tos \
    --email <email>@gmail.com \
    --redirect

# Nginx neu laden
sudo systemctl reload nginx

# DuckDNS IP manuell updaten
```

**DONE!** ✅

### 3️⃣ Status prüfen

```bash
./check-deployment.sh
```

**Fertig! 🎉**

---

## 🌐 URLs

Nach dem Deployment:

```
Frontend:  http://VM_IP:5173
Backend:   http://VM_IP:8080
API Docs:  http://VM_IP:8080/q/swagger-ui
```

---

## 🛠️ Wichtige Befehle

```bash
# Status prüfen
./check-deployment.sh

# Images aktualisieren
./update-images.sh

# Logs anzeigen
gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a \
  --command='cd /opt/cad-travel && sudo docker-compose logs -f'

# Container neustarten
gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a \
  --command='cd /opt/cad-travel && sudo docker-compose restart'

# Deployment löschen
./destroy.sh
```

---

## 📊 Was wird deployed?

```
┌─────────────────────────────────┐
│  Compute Engine VM              │
│  ├─ Backend  (Port 8080)        │
│  └─ Frontend (Port 5173)        │
└─────────────────────────────────┘
           │
           ├──► Cloud SQL (PostgreSQL)
           ├──► Cloud Storage (Bilder)
           └──► Firestore (NoSQL)
```

---

## 💰 Kosten

| Ressource | Kosten/Monat |
|-----------|--------------|
| VM (e2-medium) | ~25€ |
| Cloud SQL (f1-micro) | ~15€ |
| Storage | ~0.02€/GB |
| **Total** | **~40-50€** |

---

## 🐛 Troubleshooting

### Authentifizierung fehlt
```bash
gcloud auth application-default login
```

### APIs nicht aktiviert
```bash
gcloud services enable compute.googleapis.com sqladmin.googleapis.com storage.googleapis.com
```

### `$'\r': command not found` Fehler
**Problem:** Windows line endings im startup-script.sh

**Lösung:**
```bash
# VS Code: Klicke unten rechts auf CRLF → Wähle LF → Speichern
# Oder in Git Bash:
sed -i 's/\r$//' startup-script.sh
terraform apply
```

### Post-Deploy SSH Timeout (Windows)
**Problem:** PuTTY plink Host Key Prompt

**Lösung:** Script erkennt Windows automatisch. Falls Probleme:
```bash
echo "y" | gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a --command="echo test"
```

### Container starten nicht
```bash
./check-deployment.sh
# Prüfe Logs für Fehler

# Startup Script Logs anzeigen
gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a \
  --command='sudo cat /var/log/startup-script.log'
```

### Post-Deploy manuell ausführen
```bash
# Falls das automatische Post-Deploy fehlschlägt
./post-deploy.sh
```

---

## 📚 Weitere Infos

Siehe `README_IAAS_DEPLOYMENT.md` für:
- Detaillierte Architektur
- Manuelle Deployment-Schritte
- Security Best Practices
- Wartung & Updates

---

**Viel Erfolg! 🚀**

