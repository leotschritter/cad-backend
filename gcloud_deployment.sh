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

gcloud services enable artifactregistry.googleapis.com

gcloud artifacts repositories create docker-repo --repository-format=docker --location=europe-west1 --description="Docker repository for travel app"

gcloud auth configure-docker europe-west1-docker.pkg.dev

gcloud services enable run.googleapis.com

# Build and push Docker image
 docker build -t europe-west1-docker.pkg.dev/$PROJECT_ID/docker-repo/travel-backend:"$VERSION" .
 docker push europe-west1-docker.pkg.dev/$PROJECT_ID/docker-repo/travel-backend:"$VERSION"

# Deploy to Cloud Run with Cloud SQL connection
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
#     --set-env-vars "QUARKUS_PROFILE=prod"
