#!/bin/bash
set -e

echo "=== CAD Application Deployment ==="
echo "Starting at: $(date)"

# System Update
apt-get update
DEBIAN_FRONTEND=noninteractive apt-get upgrade -y

# Install Docker
apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Start Docker
systemctl start docker
systemctl enable docker

# Create application directory
mkdir -p /opt/cad
cd /opt/cad

# Create docker-compose.yml with injected variables
cat > docker-compose.yml <<'EOF'
services:
  postgres:
    image: postgres:16
    container_name: postgres_db
    environment:
      POSTGRES_USER: ${db_user}
      POSTGRES_PASSWORD: ${db_password}
      POSTGRES_DB: ${db_name}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${db_user} -d ${db_name}"]
      interval: 5s
      timeout: 5s
      retries: 20
    restart: unless-stopped

  backend:
    image: ${backend_image}
    container_name: backend
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      QUARKUS_DATASOURCE_JDBC_URL: "jdbc:postgresql://postgres:5432/${db_name}"
      QUARKUS_DATASOURCE_USERNAME: "${db_user}"
      QUARKUS_DATASOURCE_PASSWORD: "${db_password}"
    restart: unless-stopped

  frontend:
    image: ${frontend_image}
    container_name: frontend
    ports:
      - "5173:8080"
    depends_on:
      - backend
    restart: unless-stopped

volumes:
  postgres_data:
EOF

# Pull images and start services
echo "Pulling Docker images..."
docker compose pull

echo "Starting services..."
docker compose up -d

# Wait for services to be healthy
echo "Waiting for services to be ready..."
sleep 10

# Show status
docker compose ps

echo "=== Deployment completed at: $(date) ==="
echo "Backend URL: http://$(curl -s http://metadata.google.internal/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip -H "Metadata-Flavor: Google"):8080"
echo "Frontend URL: http://$(curl -s http://metadata.google.internal/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip -H "Metadata-Flavor: Google"):5173"

# Setup auto-restart on reboot
cat > /etc/systemd/system/cad-app.service <<'SERVICE'
[Unit]
Description=CAD Application
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/cad
ExecStart=/usr/bin/docker compose up -d
ExecStop=/usr/bin/docker compose down

[Install]
WantedBy=multi-user.target
SERVICE

systemctl daemon-reload
systemctl enable cad-app.service
