# Google Cloud SQL Configuration Guide

## Overview
This application is now configured to work with both:
1. **Local Development**: Docker PostgreSQL container
2. **Production**: Google Cloud SQL

The configuration uses environment variables to switch between environments seamlessly.

## Environment Variables

### Required Environment Variables for Cloud SQL:
- `DB_USER`: Database username
- `DB_PASSWORD`: Database password
- `DB_URL`: JDBC connection URL with Cloud SQL socket factory

### Local Development (Default)
No environment variables needed. Uses defaults:
- `DB_USER=myuser`
- `DB_PASSWORD=mypassword`
- `DB_URL=jdbc:postgresql://localhost:5432/mydatabase`

### Production (Google Cloud SQL)
Set these environment variables:
```bash
export DB_USER=cad_db_user
export DB_PASSWORD="h9K#NGBG8j39QpBBti&Q"
export DB_URL="jdbc:postgresql:///travel-db?cloudSqlInstance=graphite-plane-474510-s9:europe-west1:cad-travel-db&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
export QUARKUS_PROFILE=prod
```

## Connection Methods

### Method 1: Cloud SQL Socket Factory (Recommended for Cloud Run)
This method uses the Google Cloud SQL JDBC Socket Factory to connect securely without needing IP whitelisting.

**JDBC URL Format:**
```
jdbc:postgresql:///<database-name>?cloudSqlInstance=<connection-name>&socketFactory=com.google.cloud.sql.postgres.SocketFactory
```

**Example:**
```
jdbc:postgresql:///travel-db?cloudSqlInstance=graphite-plane-474510-s9:europe-west1:cad-travel-db&socketFactory=com.google.cloud.sql.postgres.SocketFactory
```

### Method 2: Cloud SQL Proxy (for local testing)
If you want to test Cloud SQL connection locally:

```bash
# Download Cloud SQL Proxy
wget https://dl.google.com/cloudsql/cloud_sql_proxy.linux.amd64 -O cloud_sql_proxy
chmod +x cloud_sql_proxy

# Run the proxy
./cloud_sql_proxy -instances=graphite-plane-474510-s9:europe-west1:cad-travel-db=tcp:5432

# In another terminal, set environment variables
export DB_USER=cad_db_user
export DB_PASSWORD="h9K#NGBG8j39QpBBti&Q"
export DB_URL="jdbc:postgresql://localhost:5432/travel-db"

# Run your application
./mvnw quarkus:dev
```

## Deployment to Cloud Run

### Step 1: Build and push Docker image
```bash
PROJECT_ID="graphite-plane-474510-s9"
docker build -t europe-west1-docker.pkg.dev/$PROJECT_ID/docker-repo/travel-backend:latest .
docker push europe-west1-docker.pkg.dev/$PROJECT_ID/docker-repo/travel-backend:latest
```

### Step 2: Deploy to Cloud Run
```bash
gcloud run deploy travel-backend \
    --image europe-west1-docker.pkg.dev/$PROJECT_ID/docker-repo/travel-backend:latest \
    --region europe-west1 \
    --platform managed \
    --allow-unauthenticated \
    --service-account cad-travel-run-sa@${PROJECT_ID}.iam.gserviceaccount.com \
    --add-cloudsql-instances graphite-plane-474510-s9:europe-west1:cad-travel-db \
    --set-env-vars "DB_USER=cad_db_user" \
    --set-env-vars "DB_PASSWORD=h9K#NGBG8j39QpBBti&Q" \
    --set-env-vars "DB_URL=jdbc:postgresql:///travel-db?cloudSqlInstance=graphite-plane-474510-s9:europe-west1:cad-travel-db&socketFactory=com.google.cloud.sql.postgres.SocketFactory" \
    --set-env-vars "QUARKUS_PROFILE=prod"
```

## Security Best Practices

### Using Google Secret Manager (Recommended for production)
Instead of passing passwords directly, use Secret Manager:

```bash
# Create secret
echo -n "h9K#NGBG8j39QpBBti&Q" | gcloud secrets create db-password --data-file=-

# Grant access to service account
gcloud secrets add-iam-policy-binding db-password \
    --member="serviceAccount:cad-travel-run-sa@graphite-plane-474510-s9.iam.gserviceaccount.com" \
    --role="roles/secretmanager.secretAccessor"

# Deploy with secret
gcloud run deploy travel-backend \
    --image europe-west1-docker.pkg.dev/graphite-plane-474510-s9/docker-repo/travel-backend:latest \
    --region europe-west1 \
    --platform managed \
    --allow-unauthenticated \
    --service-account cad-travel-run-sa@graphite-plane-474510-s9.iam.gserviceaccount.com \
    --add-cloudsql-instances graphite-plane-474510-s9:europe-west1:cad-travel-db \
    --set-env-vars "DB_USER=cad_db_user" \
    --set-secrets "DB_PASSWORD=db-password:latest" \
    --set-env-vars "DB_URL=jdbc:postgresql:///travel-db?cloudSqlInstance=graphite-plane-474510-s9:europe-west1:cad-travel-db&socketFactory=com.google.cloud.sql.postgres.SocketFactory" \
    --set-env-vars "QUARKUS_PROFILE=prod"
```

## Testing the Configuration

### Test locally with Docker PostgreSQL:
```bash
docker-compose up -d postgres
./mvnw quarkus:dev
```

### Test with Cloud SQL (using proxy):
```bash
./cloud_sql_proxy -instances=graphite-plane-474510-s9:europe-west1:cad-travel-db=tcp:5432 &
export DB_USER=cad_db_user
export DB_PASSWORD="h9K#NGBG8j39QpBBti&Q"
export DB_URL="jdbc:postgresql://localhost:5432/travel-db"
./mvnw quarkus:dev
```

## Troubleshooting

### Connection refused
- Ensure Cloud SQL instance is running
- Check that the service account has `roles/cloudsql.client` role
- Verify the connection name format: `PROJECT:REGION:INSTANCE`

### Authentication failed
- Verify DB_USER and DB_PASSWORD are correct
- Check if user exists in Cloud SQL: `gcloud sql users list --instance=cad-travel-db`

### Socket factory not found
- Ensure `cloud-sql-connector-jdbc` dependency is in pom.xml
- Rebuild the application: `./mvnw clean package`

## Changes Made

### 1. pom.xml
Added Google Cloud SQL JDBC Socket Factory dependency:
```xml
<dependency>
    <groupId>com.google.cloud.sql</groupId>
    <artifactId>cloud-sql-connector-jdbc</artifactId>
    <version>1.20.1</version>
</dependency>
```

### 2. application.yaml
Configured to use environment variables with fallback to local defaults:
```yaml
quarkus:
  datasource:
    username: ${DB_USER:myuser}
    password: ${DB_PASSWORD:mypassword}
    jdbc:
      url: ${DB_URL:jdbc:postgresql://localhost:5432/mydatabase}
```

### 3. gcloud_deployment.sh
Updated with Cloud Run deployment commands including environment variables.

### 4. .env.example
Created example environment file showing both local and Cloud SQL configurations.

