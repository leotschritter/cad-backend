# Terraform Setup Guide

This document outlines the **one-time manual setup steps** required before using Terraform in this repository. These steps must be completed outside of the Terraform configuration itself.

## Prerequisites

- Google Cloud SDK (`gcloud`) installed and configured
- Terraform >= 1.0 installed
- Access to the GCP project with appropriate permissions
- A GCP project ID

## Step 1: Create Terraform State Bucket

The Terraform state must be stored in a Google Cloud Storage bucket. This bucket must be created **manually** before the first Terraform run.

### Create the State Bucket

```bash
# Set your project ID
export PROJECT_ID="your-gcp-project-id"
export STATE_BUCKET="${PROJECT_ID}-terraform-state"

# Create the bucket
gsutil mb -p ${PROJECT_ID} -l europe-west1 gs://${STATE_BUCKET}

# Enable versioning (recommended for state files)
gsutil versioning set on gs://${STATE_BUCKET}

# Enable object lifecycle management (optional - to reduce costs)
cat > lifecycle.json << EOF
{
  "lifecycle": {
    "rule": [
      {
        "action": {"type": "Delete"},
        "condition": {"age": 90, "numNewerVersions": 5}
      }
    ]
  }
}
EOF
gsutil lifecycle set lifecycle.json gs://${STATE_BUCKET}
rm lifecycle.json
```

### Configure Backend

After creating the bucket, configure the backend:

1. Copy the example backend configuration:

   ```bash
   cp terraform/backend-config.hcl.example terraform/backend-config.hcl
   ```

2. Edit `terraform/backend-config.hcl` and set your bucket name:

   ```hcl
   bucket = "your-project-terraform-state"
   prefix = "terraform/state"
   ```

3. **Important**: Add `backend-config.hcl` to `.gitignore` (it should not be committed)

## Step 2: Configure CI/CD Secrets

If using GitHub Actions (or another CI/CD platform), configure the following secrets:

### Required GitHub Secrets

1. **GCP_PROJECT_ID**: Your GCP project ID
   - Settings → Secrets and variables → Actions → New repository secret
   - Name: `GCP_PROJECT_ID`
   - Value: `your-gcp-project-id`

2. **GCP_SA_KEY**: Service account key JSON for Terraform operations
   - Create a service account with these roles:
     - `roles/editor` (or more specific roles)
     - `roles/iam.serviceAccountAdmin`
     - `roles/resourcemanager.projectIamAdmin`
     - `roles/storage.admin` (for state bucket access)
   - Generate a key:
 
     ```bash
     gcloud iam service-accounts create terraform-ci \
       --display-name="Terraform CI/CD Service Account"
     
     gcloud projects add-iam-policy-binding ${PROJECT_ID} \
       --member="serviceAccount:terraform-ci@${PROJECT_ID}.iam.gserviceaccount.com" \
       --role="roles/editor"
     
     gcloud iam service-accounts keys create terraform-ci-key.json \
       --iam-account=terraform-ci@${PROJECT_ID}.iam.gserviceaccount.com
     ```

   - Copy the contents of `terraform-ci-key.json` to GitHub secret `GCP_SA_KEY`
   - **Delete the key file locally** (it's now stored securely in GitHub)

3. **TF_STATE_BUCKET**: The name of your Terraform state bucket
   - Name: `TF_STATE_BUCKET`
   - Value: `your-project-terraform-state`

4. **TF_VAR_DB_PASSWORD**: Database password (optional if auto-generation is acceptable)
   - Name: `TF_VAR_DB_PASSWORD`
   - Value: Your database password (or leave unset to auto-generate)

### Alternative: Using Google Secret Manager

Instead of GitHub secrets, you can use Google Secret Manager:

```bash
# Store secrets in Secret Manager
echo -n "your-db-password" | gcloud secrets create tf-var-db-password \
  --data-file=- \
  --replication-policy="automatic"

# Grant access to the CI service account
gcloud secrets add-iam-policy-binding tf-var-db-password \
  --member="serviceAccount:terraform-ci@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

Then update the CI/CD workflow to fetch secrets from Secret Manager instead of GitHub secrets.

## Step 3: Local Development Setup

For local development, configure authentication:

```bash
# Authenticate with Google Cloud
gcloud auth application-default login

# Set your project
gcloud config set project ${PROJECT_ID}
```

### Local Variables

1. Copy the example variables file:

   ```bash
   cp terraform/terraform.tfvars.example terraform/terraform.tfvars
   ```

2. Edit `terraform.tfvars` with your values (but **never** include `db_password`)

3. Set sensitive variables via environment variables:

   ```bash
   export TF_VAR_db_password="your-secure-password"
   ```

   Or use a `.env` file (ensure it's in `.gitignore`):

   ```bash
   # .env (DO NOT COMMIT)
   export TF_VAR_db_password="your-secure-password"
   source .env
   ```

## Step 4: Initialize Terraform

After completing the above steps, initialize Terraform:

```bash
cd terraform

# Initialize with backend configuration
terraform init -backend-config=backend-config.hcl

# Verify backend is configured
terraform init -backend-config=backend-config.hcl -reconfigure
```

## Step 5: First Terraform Run

### Plan First

```bash
# Review what will be created
terraform plan
```

### Apply

```bash
# Apply the configuration
terraform apply
```

Type `yes` when prompted, or use `-auto-approve` for non-interactive runs.

## Importing Existing Resources

If you have existing resources that should be managed by Terraform:

### Option 1: Manual Import (Recommended)

Import resources one at a time:

```bash
# Example: Import existing Cloud SQL instance
terraform import module.database.google_sql_database_instance.main PROJECT_ID/INSTANCE_NAME

# Example: Import existing service account
terraform import module.iam.google_service_account.kubernetes_sa projects/PROJECT_ID/serviceAccounts/EMAIL
```

### Option 2: Import Blocks (Terraform 1.5+)

Create an `imports.tf` file with import blocks:

```hcl
import {
  to = module.database.google_sql_database_instance.main
  id = "PROJECT_ID/INSTANCE_NAME"
}
```

Then run `terraform plan` to see the differences and adjust your configuration accordingly.

**Important**: After importing, review the plan carefully and adjust resource configurations to match the imported state.

## Verification

After setup, verify everything works:

```bash
# Check state location
terraform state list

# View outputs
terraform output

# Validate configuration
terraform validate

# Format check
terraform fmt -check -recursive
```

## Troubleshooting

### Backend Configuration Error

If you see errors about the backend:

1. Verify the bucket exists: `gsutil ls gs://${STATE_BUCKET}`
2. Check permissions: Ensure your account has `roles/storage.admin` on the bucket
3. Verify backend-config.hcl is correct

### Authentication Errors

```bash
# Re-authenticate
gcloud auth application-default login

# Verify project
gcloud config get-value project
```

### State Lock Issues

If Terraform state is locked (e.g., from a failed run):

```bash
# List locks (if using state locking)
gsutil ls gs://${STATE_BUCKET}/terraform/state/.terraform.tfstate.lock.info

# Force unlock (use with caution)
terraform force-unlock LOCK_ID
```

### Missing Secrets

If CI/CD fails due to missing secrets:

1. Verify all required secrets are set in GitHub (Settings → Secrets)
2. Check secret names match the workflow file
3. For local development, ensure environment variables are set

## Security Best Practices

1. **Never commit**:
   - `terraform.tfvars` (if it contains sensitive data)
   - `backend-config.hcl`
   - `.tfstate` files
   - Service account keys

2. **Use Secret Manager** for production secrets instead of CI/CD variables when possible

3. **Rotate secrets regularly**, especially service account keys

4. **Enable audit logging** for the state bucket:
   ```bash
   gsutil logging set on -b gs://${STATE_BUCKET}-logs gs://${STATE_BUCKET}
   ```

5. **Restrict access** to the state bucket:
   ```bash
   # Only allow specific service accounts
   gsutil iam ch serviceAccount:terraform-ci@${PROJECT_ID}.iam.gserviceaccount.com:roles/storage.objectAdmin gs://${STATE_BUCKET}
   ```

## Next Steps

After completing setup:

1. Review the [README.md](./README.md) for usage instructions
2. Check [QUICKSTART.md](./QUICKSTART.md) for deployment workflows
3. Configure environment-specific variable files if needed
4. Set up monitoring and alerting for your infrastructure

## Support

For issues or questions:
- Check Terraform logs: `terraform plan -detailed-exitcode`
- Review GCP audit logs for permission issues
- Consult [Terraform GCP Provider documentation](https://registry.terraform.io/providers/hashicorp/google/latest/docs)

