# CAD Travel App - IaaS Deployment Guide

## 📋 Übersicht

Dieses Terraform-Setup deployed die CAD Travel App als **IaaS (Infrastructure as a Service)** auf Google Cloud Platform mit:

- ✅ **Compute Engine VM** (Ubuntu 22.04) mit Docker + Docker Compose
- ✅ **Cloud SQL** (PostgreSQL) für die Datenbank
- ✅ **Cloud Storage** für Bilder
- ✅ **Firestore** für NoSQL-Daten
- ✅ **Service Accounts** mit minimalen Berechtigungen
- ✅ **Automatisches Deployment** via Startup Script

---

## 🚀 Schnellstart (3 Schritte)

### 1. Konfiguration anpassen

Erstelle `terraform/terraform.tfvars`:

```hcl
project_id = "graphite-plane-474510-s9"
region     = "europe-west3"
zone       = "europe-west3-a"

# Optional: Anpassen
vm_machine_type = "e2-medium"
db_tier         = "db-f1-micro"
```

### 2. Deployment starten

```bash
cd terraform/
chmod +x *.sh
./deploy.sh
```

### 3. Status prüfen

```bash
./check-deployment.sh
```

**Fertig!** 🎉

---

## 📂 Dateistruktur

```
terraform/
├── main.tf              # Provider & APIs
├── variables.tf         # Input Variablen
├── database.tf          # Cloud SQL PostgreSQL
├── storage.tf           # Cloud Storage Bucket
├── iam.tf               # Service Accounts & Rollen
├── compute.tf           # VM + Firewall
├── outputs.tf           # Deployment Summary
├── startup-script.sh    # VM Startup Script (installiert Docker, startet Container)
├── deploy.sh            # Haupt-Deployment Script (Terraform + Post-Deploy)
├── post-deploy.sh       # Post-Deployment Script (führt startup-script aus)
├── check-deployment.sh  # Status prüfen
├── update-images.sh     # Images aktualisieren
├── destroy.sh           # Alles löschen
└── terraform.tfvars     # Konfiguration (nicht in Git!)
```

### 🔄 Deployment Scripts Erklärt

#### `deploy.sh` - Haupt-Deployment Script
Das zentrale Script für das komplette Deployment:
1. **Authentifizierung prüfen** - Stellt sicher, dass gcloud konfiguriert ist
2. **APIs aktivieren** - Aktiviert benötigte GCP APIs
3. **Terraform initialisieren** - Führt `terraform init` aus
4. **Ressourcen importieren** - Importiert existierende Ressourcen (falls vorhanden)
5. **Terraform Plan** - Erstellt Ausführungsplan
6. **Terraform Apply** - Deployed die Infrastruktur
7. **Post-Deploy ausführen** - Führt automatisch `post-deploy.sh` aus

```bash
./deploy.sh
# Optional: Post-Deploy überspringen
SKIP_POST_DEPLOY=true ./deploy.sh
```

#### `startup-script.sh` - VM Startup Script
Wird auf der VM ausgeführt und installiert die komplette Application:
1. **System-Pakete** - Installiert Docker, docker-compose, nginx, certbot
2. **Docker Service** - Startet Docker Daemon
3. **App-Verzeichnis** - Erstellt `/opt/cad-travel`
4. **GCP Credentials** - Schreibt Service Account Key
5. **Environment Config** - Generiert `.env` mit DB-Credentials (aus Terraform)
6. **Docker Compose File** - Erstellt `docker-compose.yml` mit Service Definitionen
7. **Nginx Reverse Proxy** - Konfiguriert Routing (`/` → Frontend, `/api` → Backend)
8. **Container starten** - Pulled Images und startet Backend + Frontend Container

**Wichtig:** Script wird in VM Metadata gespeichert und von `post-deploy.sh` ausgeführt.

#### `post-deploy.sh` - Post-Deployment Automation
Führt das Startup-Script auf der VM aus **nach** Terraform Deployment:
1. **VM Status prüfen** - Stellt sicher, dass VM läuft
2. **SSH Verfügbarkeit** - Wartet bis SSH bereit ist (Auto-Accept Host Key auf Windows)
3. **Startup Script abrufen** - Holt Script aus VM Metadata via `curl`
4. **Line Endings fixen** - Konvertiert CRLF zu LF mit `tr -d '\r'`
5. **Script ausführen** - Führt Installation und Deployment auf VM aus
6. **Deployment verifizieren** - Prüft Container Status

```bash
# Manuell ausführen (nach terraform apply)
./post-deploy.sh
```

**Cross-Platform Support:**
- Linux/Mac: Nutzt OpenSSH mit `-o` Flags
- Windows (Git Bash/WSL): Erkennt Windows automatisch, akzeptiert SSH Host Key mit PuTTY plink

---

## 🛠️ Manuelle Schritte

### 1. Authentifizierung

```bash
gcloud auth application-default login
gcloud config set project graphite-plane-474510-s9
```

### 2. APIs aktivieren

```bash
gcloud services enable compute.googleapis.com \
  sqladmin.googleapis.com \
  storage.googleapis.com \
  iam.googleapis.com \
  firestore.googleapis.com \
  servicenetworking.googleapis.com
```

### 3. Terraform initialisieren

```bash
cd terraform/
terraform init
```

### 4. Plan erstellen

```bash
terraform plan -out=tfplan
```

### 5. Deployment durchführen

```bash
terraform apply tfplan
```

### 6. Outputs anzeigen

```bash
terraform output deployment_summary
```

---

## 🔧 Wartung & Updates

### Container Logs anzeigen

```bash
./check-deployment.sh
```

Oder manuell:

```bash
gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a \
  --command='cd /opt/cad-travel && sudo docker-compose logs -f'
```

### Images aktualisieren

```bash
./update-images.sh
```

Oder manuell:

```bash
gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a \
  --command='cd /opt/cad-travel && sudo docker-compose pull && sudo docker-compose up -d'
```

### Container neustarten

```bash
gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a \
  --command='cd /opt/cad-travel && sudo docker-compose restart'
```

### SSH zur VM

```bash
gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a
```

---

## 🗑️ Deployment löschen

```bash
./destroy.sh
```

Oder manuell:

```bash
terraform destroy
```

---

## 📊 Architektur

```
┌─────────────────────────────────────────────┐
│  Compute Engine VM (e2-medium)              │
│  ├─ Docker                                  │
│  │  ├─ Backend Container (Port 8080)        │
│  │  └─ Frontend Container (Port 5173)       │
│  └─ Service Account (minimale Rechte)       │
└─────────────────────────────────────────────┘
           │
           ├──────► Cloud SQL (PostgreSQL)
           ├──────► Firestore (NoSQL)
           └──────► Cloud Storage (Bilder)
```

---

## 🔐 Sicherheit

- ✅ Service Account mit minimalen Berechtigungen
- ✅ Firewall nur für Port 5173, 8080, 22
- ✅ SSH nur von definierten IPs (anpassbar in `variables.tf`)
- ✅ DB Passwort automatisch generiert
- ✅ Service Account Key als Secret

### SSH-Zugriff einschränken

In `terraform.tfvars`:

```hcl
allowed_ssh_ips = ["DEINE_IP/32"]
```

---

## 💰 Kosten (ca.)

| Ressource | Typ | Kosten/Monat |
|-----------|-----|--------------|
| VM | e2-medium | ~25€ |
| Cloud SQL | db-f1-micro | ~15€ |
| Storage | Standard | ~0.02€/GB |
| Firestore | Native | Pay-per-use |
| **Total** | | **~40-50€** |

**Kostenoptimierung:**
- Nutze `e2-micro` VM für Tests (~7€/Monat)
- Aktiviere Cloud SQL Auto-Shutdown
- Setze Storage Lifecycle Policies

---

## 🐛 Troubleshooting

### Post-Deploy Script Fehler

#### `$'\r': command not found` Fehler
**Problem:** `startup-script.sh` hat Windows line endings (CRLF)

**Lösung 1 - VS Code:**
1. Öffne `startup-script.sh`
2. Klicke unten rechts auf `CRLF`
3. Wähle `LF`
4. Speichern

**Lösung 2 - Git Bash:**
```bash
cd terraform
sed -i 's/\r$//' startup-script.sh
terraform apply
```

**Lösung 3 - Fix Script:**
```bash
./fix-line-endings.sh
terraform apply
```

Siehe `LINE_ENDINGS_FIX.md` für Details.

#### SSH Connection Timeout
**Problem:** Post-Deploy kann nicht zur VM verbinden

**Lösung:**
```bash
# Manuell testen
gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a

# Windows: Host Key manuell akzeptieren
echo "y" | gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a --command="echo test"
```

Siehe `WINDOWS_PLINK_FIX.md` für Windows-spezifische Probleme.

#### `plink: unknown option "-o"` (Windows)
**Problem:** Windows nutzt PuTTY plink statt OpenSSH

**Lösung:** Script erkennt Windows automatisch und nutzt korrektes Format.
Falls Probleme bestehen:
```bash
# OpenSSH statt PuTTY nutzen
gcloud config set ssh/putty false
```

### Container starten nicht

```bash
# Logs prüfen
./check-deployment.sh

# Manuell auf VM
gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a
cd /opt/cad-travel
sudo docker-compose logs

# Startup Script Logs
gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a \
  --command='sudo cat /var/log/startup-script.log'
```

### DB Connection Fehler

```bash
# Cloud SQL IP prüfen
terraform output db_public_ip

# Firewall prüfen
gcloud sql instances describe cad-travel-db --format="get(settings.ipConfiguration)"

# .env File auf VM prüfen
gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a \
  --command='sudo cat /opt/cad-travel/.env'
```

### Images können nicht gepullt werden

```bash
# Auf VM testen
gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a
docker pull ghcr.io/leotschritter/cad-backend:iaas-latest
docker pull ghcr.io/leotschritter/cad-frontend:iaas-latest

# Docker Login prüfen (falls private Images)
docker login ghcr.io
```

### Firestore 409 Fehler

Firestore existiert bereits:

```bash
terraform import google_firestore_database.database "projects/graphite-plane-474510-s9/databases/(default)"
```

### Startup Script wird nicht ausgeführt

**Problem:** VM wurde neu erstellt, aber Startup Script läuft nicht

**Lösung:**
```bash
# Manuell Post-Deploy ausführen
./post-deploy.sh

# Oder Script direkt auf VM ausführen
gcloud compute ssh cad-travel-app-vm --zone=europe-west3-a \
  --command="curl -s -H 'Metadata-Flavor: Google' http://metadata.google.internal/computeMetadata/v1/instance/attributes/startup-script | tr -d '\r' | sudo bash -s"
```

---

## 📝 Environment Variables

### In Terraform (`variables.tf`)
- `project_id` - GCP Project ID
- `region` - GCP Region
- `zone` - GCP Zone
- `vm_machine_type` - VM Größe
- `backend_image` - Backend Docker Image
- `frontend_image` - Frontend Docker Image

### Auf der VM (`.env`)
- `DB_HOST` - Cloud SQL IP
- `DB_NAME` - Datenbankname
- `DB_USER` - DB Username
- `DB_PASSWORD` - DB Passwort
- `PROJECT_ID` - GCP Project ID
- `STORAGE_BUCKET` - Bucket Name

### In Containern (`docker-compose.yml`)
- `QUARKUS_DATASOURCE_*` - DB Connection
- `GOOGLE_APPLICATION_CREDENTIALS` - Service Account Key
- `GOOGLE_CLOUD_PROJECT` - GCP Project
- `GCS_BUCKET_NAME` - Storage Bucket

---

## 🎯 Best Practices

✅ **DO:**
- Nutze `terraform.tfvars` für Secrets (nicht in Git!)
- Erstelle Backups vor Updates
- Monitore Logs regelmäßig
- Aktiviere Cloud SQL Backups
- Nutze Terraform State Backend (GCS)

❌ **DON'T:**
- Service Account Keys nicht in Git committen
- DB Passwörter nicht hardcoden
- Nicht `terraform destroy` ohne Backup
- SSH nicht für alle IPs öffnen

---

## 📞 Support

Bei Problemen:

1. Prüfe Logs: `./check-deployment.sh`
2. Suche in Terraform State: `terraform state list`
3. Prüfe GCP Console
4. Siehe Troubleshooting oben

---

## 📄 Lizenz

Dieses Deployment-Setup ist Teil des CAD Travel App Projekts.

---

**Viel Erfolg mit deinem Deployment! 🚀**

