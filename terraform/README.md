# Terraform Infrastructure for Travel App Backend

This directory contains Terraform configuration files for automated deployment of the Travel App Backend to Google Cloud Platform.

## 📋 Prerequisites

1. **Terraform**: Install Terraform >= 1.0
   ```bash
   # Download from https://www.terraform.io/downloads
   ```

2. **Google Cloud SDK**: Install and configure gcloud CLI
   ```bash
   gcloud auth application-default login
   gcloud config set project YOUR_PROJECT_ID
   ```

3. **Docker**: Required for building and pushing container images

## 🏗️ Infrastructure Components

This Terraform configuration creates the following GCP resources:

- **Cloud Run**: Serverless container platform for the application
- **Cloud SQL (PostgreSQL 16)**: Managed database instance
- **Firestore**: NoSQL document database
- **Cloud Storage**: Object storage bucket for images
- **Artifact Registry**: Docker container registry
- **Service Account**: With appropriate IAM roles
- **Secret Manager**: Secure storage for database credentials
- **API Services**: Enables required Google Cloud APIs

## 📁 File Structure

```
terraform/
├── provider.tf              # Terraform and provider configuration
├── variables.tf             # Input variable definitions
├── main.tf                  # Main infrastructure resources
├── outputs.tf               # Output values
├── terraform.tfvars.example # Example variable values
└── ...
```

## 🚀 Quick Start

### 1. Initialize Terraform

```bash
cd terraform
terraform init
```

### 2. Configure Variables

Copy the example variables file and customize it:

```bash
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars` with your values:
```hcl
project_id = "your-gcp-project-id"
region     = "europe-west1"
```

### 3. Validate Configuration

Validate your Terraform configuration:

```bash
terraform validate
```

### 4. Review the Plan

Preview the changes Terraform will make:

```bash
terraform plan
```

### 4. Apply the Configuration

Create the infrastructure:

```bash
terraform apply
```

Type `yes` when prompted to confirm.

### 5. Build and Deploy Application

After infrastructure is created, get the deployment commands:

```bash
terraform output deployment_commands
```

Then build and deploy your Docker image:

```bash
# Authenticate with Artifact Registry
gcloud auth configure-docker europe-west1-docker.pkg.dev

# Build the Docker image
docker build -t europe-west1-docker.pkg.dev/YOUR_PROJECT_ID/docker-repo/travel-backend:latest .

# Push the image
docker push europe-west1-docker.pkg.dev/YOUR_PROJECT_ID/docker-repo/travel-backend:latest

# Trigger a new Cloud Run revision (if needed)
gcloud run services update travel-backend \
  --image europe-west1-docker.pkg.dev/YOUR_PROJECT_ID/docker-repo/travel-backend:latest \
  --region europe-west1
```

## 📊 View Outputs

After deployment, view the created resources:

```bash
# View all outputs
terraform output

# View specific outputs
terraform output cloud_run_url
terraform output service_urls
```

## 🔧 Configuration Options

### Environment-Specific Deployments

Create different variable files for different environments:

```bash
# Development
terraform apply -var-file="dev.tfvars"

# Staging
terraform apply -var-file="staging.tfvars"

# Production
terraform apply -var-file="prod.tfvars"
```

### Key Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `project_id` | GCP project ID | **Required** |
| `region` | GCP region | `europe-west1` |
| `app_name` | Application name | `travel-backend` |
| `app_version` | Docker image tag | `latest` |
| `db_tier` | Cloud SQL instance tier | `db-f1-micro` |
| `cloud_run_memory` | Memory allocation | `512Mi` |
| `allow_unauthenticated` | Public access | `true` |

See `variables.tf` for all available options.

## 🔐 Security Considerations

1. **Database Password**: Automatically generated and stored in Secret Manager
2. **Service Account**: Principle of least privilege with specific IAM roles
3. **Secret Access**: Only the Cloud Run service account can access secrets
4. **Network**: Cloud SQL uses private networking when available

### Retrieve Database Password

```bash
terraform output db_password_secret_id
gcloud secrets versions access latest --secret="travel-backend-db-password"
```

## 🔄 Updating Infrastructure

### Modify Resources

1. Edit the Terraform files or variables
2. Review changes: `terraform plan`
3. Apply changes: `terraform apply`

### Update Application

To deploy a new version of the application:

```bash
# Build and push with new tag
docker build -t europe-west1-docker.pkg.dev/PROJECT_ID/docker-repo/travel-backend:v2.0 .
docker push europe-west1-docker.pkg.dev/PROJECT_ID/docker-repo/travel-backend:v2.0

# Update Terraform variable
echo 'app_version = "v2.0"' >> terraform.tfvars

# Apply (only Cloud Run service will update)
terraform apply
```

## 🗑️ Destroying Infrastructure

**Warning**: This will delete all resources including data!

```bash
# Preview what will be destroyed
terraform plan -destroy

# Destroy all resources
terraform destroy
```

**Note**: Cloud SQL instances have deletion protection enabled by default. To destroy them, first disable protection in `main.tf`:

```hcl
resource "google_sql_database_instance" "main" {
  # ...
  deletion_protection = false
}
```

## 🐛 Troubleshooting

### API Not Enabled Error

If you get an error about APIs not being enabled:

```bash
gcloud services enable sqladmin.googleapis.com \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  firestore.googleapis.com \
  storage-api.googleapis.com \
  iamcredentials.googleapis.com \
  secretmanager.googleapis.com
```

### Permission Errors

Ensure your GCP account has the following roles:
- `roles/editor` or equivalent permissions
- `roles/iam.serviceAccountAdmin`
- `roles/resourcemanager.projectIamAdmin`

### State Lock Issues

If Terraform state is locked:

```bash
terraform force-unlock LOCK_ID
```

### Cloud Run Deployment Fails

1. Check that the Docker image exists in Artifact Registry
2. Verify the image tag matches `app_version`
3. Check Cloud Run logs:
   ```bash
   gcloud run services logs read travel-backend --region=europe-west1
   ```

## 📚 Additional Resources

- [Terraform GCP Provider Documentation](https://registry.terraform.io/providers/hashicorp/google/latest/docs)
- [Cloud Run Documentation](https://cloud.google.com/run/docs)
- [Cloud SQL Documentation](https://cloud.google.com/sql/docs)
- [Terraform Best Practices](https://www.terraform.io/docs/cloud/guides/recommended-practices/index.html)

## 🤝 Contributing

When making changes to the infrastructure:

1. Create a feature branch
2. Test changes with `terraform plan`
3. Document new variables and outputs
4. Update this README if needed
5. Submit for review

## 📝 Notes

- **State Management**: Consider using a GCS backend for team environments
- **Secrets**: Never commit `terraform.tfvars` with sensitive data to version control
- **Cost Management**: Monitor GCP costs, especially for Cloud SQL and Cloud Run
- **Backups**: Cloud SQL backups are enabled by default at 3:00 AM
- **Monitoring**: Set up Cloud Monitoring alerts for production environments

