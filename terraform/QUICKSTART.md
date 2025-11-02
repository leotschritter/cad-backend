# Terraform Quick Start Guide

This guide will help you deploy the Travel App Backend to Google Cloud Platform using Terraform.

## Prerequisites Checklist

- [ ] Terraform installed (>= 1.0)
- [ ] Google Cloud SDK (gcloud) installed and configured
- [ ] Docker installed
- [ ] GCP Project created
- [ ] Billing enabled on GCP project
- [ ] GCP credentials configured: `gcloud auth application-default login`

## Step-by-Step Deployment

### 1. Configure Your Project

Create a `terraform.tfvars` file in the `terraform/` directory:

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars` and set your project ID:

```hcl
project_id = "your-gcp-project-id"
region     = "europe-west1"
```

### 2. Authenticate with Google Cloud

**Important:** Authenticate before running Terraform commands:

```bash
gcloud auth application-default login
gcloud config set project graphite-plane-474510-s9
```

This will open your browser to sign in with Google.

### 3. Initialize Terraform

```bash
terraform init
```

This downloads the required provider plugins.

### 4. Review the Plan

```bash
terraform plan
### 5. Apply the Configuration

Review the resources that will be created:
- ✅ Cloud SQL PostgreSQL instance
- ✅ Cloud Run service
- ✅ Firestore database
- ✅ Cloud Storage bucket
- ✅ Artifact Registry
### 6. Build and Deploy Your Application
- ✅ Secret Manager for credentials

### 4. Apply the Configuration

```bash
terraform apply
```

Type `yes` when prompted. This will take 5-10 minutes.

### 5. Build and Deploy Your Application

After infrastructure is created:

```bash
# Get the image URL from Terraform outputs
terraform output docker_image_url

# Authenticate with Artifact Registry
gcloud auth configure-docker europe-west1-docker.pkg.dev
### 7. Access Your Application
# Build the Docker image (from project root)
cd ..
docker build -t europe-west1-docker.pkg.dev/YOUR_PROJECT/docker-repo/travel-backend:latest .

# Push the image
docker push europe-west1-docker.pkg.dev/YOUR_PROJECT/docker-repo/travel-backend:latest

# Cloud Run will automatically deploy the new image
```

### 6. Access Your Application

Get the Cloud Run URL:

```bash
cd terraform
terraform output cloud_run_url
```

Visit the URL in your browser. Access the API documentation at:
- Swagger UI: `https://YOUR-URL/q/swagger-ui/`
- OpenAPI spec: `https://YOUR-URL/q/openapi`

## Using the Deployment Script (Easier Way)

We provide convenience scripts for Windows and Linux/Mac:

### Windows:

```cmd
terraform-deploy.cmd check    # Check requirements
terraform-deploy.cmd full     # Full deployment
```

### Linux/Mac:

```bash
chmod +x terraform-deploy.sh
./terraform-deploy.sh check   # Check requirements
./terraform-deploy.sh full    # Full deployment
```

The `full` command will:
1. Initialize Terraform
2. Create and show the plan
3. Apply the configuration (with confirmation)
4. Build and push the Docker image
5. Deploy to Cloud Run

## Common Commands

```bash
# View all outputs
terraform output

# View specific output
terraform output cloud_run_url
terraform output service_urls

# Update infrastructure
terraform plan
terraform apply

# Deploy new application version
docker build -t europe-west1-docker.pkg.dev/PROJECT/docker-repo/travel-backend:v2.0 .
docker push europe-west1-docker.pkg.dev/PROJECT/docker-repo/travel-backend:v2.0
gcloud run services update travel-backend --image europe-west1-docker.pkg.dev/PROJECT/docker-repo/travel-backend:v2.0 --region europe-west1

# Destroy everything (WARNING: deletes all data!)
terraform destroy
```

## Troubleshooting

### "API not enabled" error

Enable required APIs:

```bash
gcloud services enable sqladmin.googleapis.com run.googleapis.com artifactregistry.googleapis.com firestore.googleapis.com storage-api.googleapis.com
```

### "Permission denied" error

Ensure your GCP account has these roles:
- Editor (or Owner)
- Service Account Admin
- Service Account User

### Cloud Run shows "Error: Revision failed"

Check logs:

```bash
gcloud run services logs read travel-backend --region europe-west1 --limit 50
```

Common issues:
- Docker image doesn't exist or has wrong tag
- Application port is not 8080
- Environment variables are incorrect

### Database connection fails

1. Verify Cloud SQL instance is running:
   ```bash
   gcloud sql instances list
   ```

2. Check the database credentials in Secret Manager:
   ```bash
   gcloud secrets versions access latest --secret="travel-backend-db-password"
   ```

## Cost Estimation

Approximate monthly costs (minimal usage):

- Cloud SQL (db-f1-micro): ~$10-15
- Cloud Run (with minimal traffic): ~$0-5
- Cloud Storage: ~$0-2
- Firestore: ~$0-1
- Artifact Registry: ~$0-1

**Total: ~$10-25/month** for a development environment

For production, consider upgrading:
- Cloud SQL to `db-g1-small` or higher
- Set `db_availability_type = "REGIONAL"` for high availability
- Increase Cloud Run min instances for better performance

## Security Best Practices

1. **Never commit `terraform.tfvars`** - it's in `.gitignore`
2. **Use separate projects** for dev/staging/prod
3. **Enable deletion protection** for production databases (already enabled)
4. **Rotate database passwords** regularly via Secret Manager
5. **Set up Cloud Monitoring** for production environments
6. **Configure backups** (daily backups are enabled by default)

## Next Steps

1. Set up CI/CD pipeline (GitHub Actions, Cloud Build)
2. Configure custom domain with Cloud Run
3. Set up monitoring and alerting
4. Configure Firestore indexes for your queries
5. Set up automated backups and disaster recovery

## Getting Help

- Terraform documentation: https://www.terraform.io/docs
- GCP documentation: https://cloud.google.com/docs
- Check the detailed [README.md](terraform/README.md) in the terraform directory

