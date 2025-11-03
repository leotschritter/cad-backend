### env variables

#gcloud services enable sqladmin.googleapis.com secretmanager.googleapis.com
#gcloud sql databases create "$DB" --instance="$INSTANCE"



#gcloud sql users create "$DB_USER" --instance="$INSTANCE" --password="$DB_PASS"
#CONN_NAME="$(gcloud sql instances describe "$INSTANCE" --format='value(connectionName)')"
#echo "Connection name: $CONN_NAME"   # looks like: PROJECT:REGION:INSTANCE

CONN_NAME=graphite-plane-474510-s9:europe-west1:cad-travel-db

# Service Account to connect SQL Database to Cloud Run=
#gcloud iam service-accounts create "$RUN_SA" --display-name="Cloud Run SA for camping app"

# Grant Cloud SQL Client role to service account
#gcloud projects add-iam-policy-binding "$PROJECT_ID" \
#    --member="serviceAccount:${RUN_SA}@${PROJECT_ID}.iam.gserviceaccount.com" \
#    --role="roles/cloudsql.client"

# Create docker repo and activate cloud run

#gcloud services enable artifactregistry.googleapis.com
#
#gcloud artifacts repositories create docker-repo --repository-format=docker --location=europe-west1 --description="Docker repository for travel app"
#
#gcloud auth configure-docker europe-west1-docker.pkg.dev
#
#gcloud services enable run.googleapis.com

## Build and push Docker image
# docker build -t de.htwg-konstanz.in/travel-backend:"$VERSION" .
###
# docker tag de.htwg-konstanz.in/travel-backend:"$VERSION" europe-west1-docker.pkg.dev/"$PROJECT_ID"/docker-repo/travel-backend:"$VERSION"
# docker push europe-west1-docker.pkg.dev/$PROJECT_ID/docker-repo/travel-backend:"$VERSION"
#er
## Deploy to Cloud Run with Cloud SQL connection
# gcloud run deploy travel-backend \
#     --image europe-west1-docker.pkg.dev/$PROJECT_ID/docker-repo/travel-backend:"$VERSION" \
#     --region $REGION \
#     --platform managed \
#     --allow-unauthenticated \
#     --service-account ${RUN_SA}@${PROJECT_ID}.iam.gserviceaccount.com \
#     --add-cloudsql-instances $CONN_NAME \
#     --set-env-vars "DB_USER=$DB_USER" \
#     --set-env-vars "DB_PASSWORD=$DB_PASS" \
#     --set-env-vars "DB_URL=jdbc:postgresql:///$DB?cloudSqlInstance=$CONN_NAME&socketFactory=com.google.cloud.sql.postgres.SocketFactory" \
#     --set-env-vars "BUCKET_NAME=$BUCKET_NAME" \
#     --set-env-vars "PROJECT_ID=$PROJECT_ID" \
#     --set-env-vars "BACKEND_URL=$BACKEND_URL" \
#     --set-env-vars "SERVICE_ACCOUNT_EMAIL=$SA_EMAIL" \
#     --set-env-vars "IDENTITY_PLATFORM_AUTH_ENABLED=true" \
#     --set-env-vars "CORS_ORIGINS=$FRONTEND_URL" \
#     --set-env-vars "QUARKUS_PROFILE=prod"
## DNS
# gcloud domains verify tripico.fun

# gcloud beta run domain-mappings create --region=europe-west1 --service travel-backend --domain api.tripico.fun

# Firestore
#gcloud services enable firestore.googleapis.com --project=${PROJECT_ID}

#gcloud firestore databases create --project=${PROJECT_ID} --location=${REGION}

# Object Storage
#gcloud storage buckets create gs://${BUCKET_NAME}


# IAM policies
#gcloud services enable iamcredentials.googleapis.com --project=$PROJECT_ID

#gcloud storage buckets add-iam-policy-binding gs://${BUCKET_NAME} \
#--member="serviceAccount:${SA_EMAIL}" \
#--role="roles/storage.objectAdmin"

#gcloud projects add-iam-policy-binding "$PROJECT_ID" \
#--member="serviceAccount:${SA_EMAIL}" \
#--role="roles/datastore.user"

#gcloud iam service-accounts add-iam-policy-binding ${SA_EMAIL} \
#  --member="serviceAccount:${SA_EMAIL}" \
#  --role="roles/iam.serviceAccountTokenCreator"


# ============================================================================
# Google Cloud Identity Platform (CIAM) Setup
# ============================================================================
# Identity Platform is Google's Customer Identity and Access Management solution
# Features: Authentication as a service, broad protocol support, multi-tenancy,
#           intelligent account protection, enterprise support and SLA
#
# Note: Firebase Authentication and Identity Platform are the same backend service.
#       You can configure via Firebase Console OR Cloud Console - both work identically.
# ============================================================================

# Enable Identity Platform API (required)
gcloud services enable identitytoolkit.googleapis.com --project=${PROJECT_ID}

# Option 1: Configure via Cloud Console (Enterprise UI)
# Visit: https://console.cloud.google.com/customer-identity/providers?project=${PROJECT_ID}

# Option 2: Configure via Firebase Console (Developer-friendly UI) 
# Visit: https://console.firebase.google.com/project/${PROJECT_ID}/authentication/providers

# Option 3: Configure via gcloud CLI (only needed once)
# gcloud identity platforms config create --project=${PROJECT_ID}

# Enable email/password sign-in provider (only needed once if using CLI)
# For production, you may want to enable additional providers:
# - Google Sign-In
# - SAML (for enterprise SSO)
# - OAuth providers (GitHub, Microsoft, etc.)

echo "Identity Platform API enabled for project: ${PROJECT_ID}"
echo "Configure authentication providers at:"
echo "  Cloud Console: https://console.cloud.google.com/customer-identity/providers?project=${PROJECT_ID}"
echo "  Firebase Console: https://console.firebase.google.com/project/${PROJECT_ID}/authentication/providers"
