#!/bin/bash
# ===================================================
# CAD Travel App - VM Startup Script
# Installiert Docker, Nginx, SSL und startet die App
# ===================================================

set -e

echo "=========================================="
echo "Starting CAD Travel App Deployment"
echo "=========================================="

# Logging aktivieren
exec > >(tee -a /var/log/startup-script.log)
exec 2>&1

# System Update
echo "[1/8] Updating system packages..."
apt-get update -qq
apt-get install -y docker.io docker-compose curl jq nginx certbot python3-certbot-nginx

# Docker starten
echo "[2/8] Starting Docker service..."
systemctl enable docker
systemctl start docker

# Docker ohne sudo nutzen
usermod -aG docker $USER || true

# App-Verzeichnis erstellen
echo "[3/8] Creating application directory..."
mkdir -p /opt/cad-travel
cd /opt/cad-travel

# Service Account Key erstellen (für Firestore)
echo "[4/8] Creating Service Account credentials..."
cat > gcp-key.json << 'KEY_EOF'
${service_account_key}
KEY_EOF
chmod 600 gcp-key.json

# Environment Variables für docker-compose
echo "[5/8] Creating environment configuration..."
cat > .env << EOF
# Database Configuration
DB_HOST=${db_instance_ip}
DB_PORT=5432
DB_NAME=${db_name}
DB_USER=${db_user}
DB_PASSWORD=${db_password}

# GCP Configuration
PROJECT_ID=${project_id}
STORAGE_BUCKET=${storage_bucket}
SERVICE_ACCOUNT_EMAIL=${service_account_email}
BUCKET_NAME=${storage_bucket}

# Docker Images
BACKEND_IMAGE=${backend_image}
FRONTEND_IMAGE=${frontend_image}
EOF

# docker-compose.yml erstellen
echo "[6/8] Creating docker-compose.yml..."
cat > docker-compose.yml << 'COMPOSE_EOF'
version: '3.8'

services:
  backend:
    image: $${BACKEND_IMAGE}
    container_name: backend
    ports:
      - "8080:8080"
    environment:
      # Database
      QUARKUS_DATASOURCE_JDBC_URL: "jdbc:postgresql://$${DB_HOST}:$${DB_PORT}/$${DB_NAME}"
      QUARKUS_DATASOURCE_USERNAME: "$${DB_USER}"
      QUARKUS_DATASOURCE_PASSWORD: "$${DB_PASSWORD}"

      QUARKUS_PROFILE: "prod"
      PROJECT_ID: "$${PROJECT_ID}"
      BUCKET_NAME: "$${STORAGE_BUCKET}"
      SERVICE_ACCOUNT_EMAIL: "$${SERVICE_ACCOUNT_EMAIL}"

      # GCP
      GOOGLE_CLOUD_PROJECT: "$${PROJECT_ID}"
      GCS_BUCKET_NAME: "$${STORAGE_BUCKET}"

      # Quarkus Config
      QUARKUS_HTTP_HOST: "0.0.0.0"
      QUARKUS_HTTP_PORT: "8080"
      QUARKUS_HTTP_CORS: "true"
      QUARKUS_HTTP_CORS_ORIGINS: "https://tripico.duckdns.org,http://tripico.duckdns.org,https://tripico.duckdns.org/api,http://localhost:8080"
      QUARKUS_HTTP_CORS_METHODS: "GET,POST,PUT,DELETE,OPTIONS"
      QUARKUS_HTTP_CORS_HEADERS: "accept,authorization,content-type,x-requested-with"
      BACKEND_URL: "https://tripico.duckdns.org/api"
    volumes:
      - ./gcp-key.json:/app/gcp-key.json:ro
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/q/health/live"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  frontend:
    image: $${FRONTEND_IMAGE}
    container_name: frontend
    ports:
      - "5173:8080"
    environment:
      PORT: 8080
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s
COMPOSE_EOF

# Nginx Reverse Proxy + SSL Setup
echo "[7/8] Configuring Nginx..."
cat > /etc/nginx/sites-available/default << 'NGINX_EOF'
server {
    listen 80;
    server_name tripico.duckdns.org;

    # Frontend
    location / {
        proxy_pass http://localhost:5173;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Backend Health + Swagger (KORRIGIERT)
    location /q/ {
        proxy_pass http://localhost:8080/q/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
NGINX_EOF

systemctl enable nginx
systemctl restart nginx

# Docker Compose starten
echo "[8/8] Starting Docker Compose..."
docker-compose --env-file .env pull
docker-compose --env-file .env up -d

# Warten auf Container-Start
echo "Waiting for containers to start..."
sleep 30

# SSL-Zertifikat installieren
#echo "Installing SSL certificate..."
#certbot --nginx \
#    -d tripico.duckdns.org \
#    --non-interactive \
#    --agree-tos \
#    --email dennishoang199@gmail.com \
#    --redirect || echo "⚠️  SSL installation failed - run manually later"

#systemctl enable certbot.timer

# Status prüfen
echo ""
echo "=========================================="
echo "Container Status:"
echo "=========================================="
docker-compose ps

echo ""
echo "=========================================="
echo "Deployment Complete!"
echo "=========================================="
echo "Frontend URL: https://tripico.duckdns.org"
echo "Backend API:  https://tripico.duckdns.org/api"
echo "API Docs:     https://tripico.duckdns.org/q/swagger-ui"
echo "=========================================="

# Logs verfolgen
nohup docker-compose logs -f > /var/log/docker-compose.log 2>&1 &
