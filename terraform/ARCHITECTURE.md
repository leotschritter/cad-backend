# Terraform Architecture

This document describes the modular architecture of the Terraform configuration.

## Module Structure

The Terraform configuration is organized into reusable modules:

```
terraform/
├── modules/
│   ├── project/          # Project-level APIs and Identity Platform
│   ├── iam/              # Service accounts and IAM bindings
│   ├── database/         # Cloud SQL instance, database, and secrets
│   ├── storage/          # Cloud Storage bucket
│   ├── firestore/        # Firestore database
│   ├── artifact-registry/ # Artifact Registry repository
│   ├── gke/              # GKE cluster and networking
│   └── api-gateway/      # API Gateway configuration
├── main.tf               # Root module orchestrating all modules
├── variables.tf          # Root module variables
├── outputs.tf            # Root module outputs
└── provider.tf           # Provider and backend configuration
```

## Module Dependencies

```
project (APIs, Identity Platform)
  ├── iam (Service Accounts)
  │     ├── database (needs service account for secret access)
  │     ├── storage (needs service account for bucket access)
  │     └── api-gateway (needs service account for backend auth)
  ├── database (Cloud SQL)
  ├── storage (Cloud Storage bucket)
  ├── firestore (Firestore database)
  ├── artifact-registry (Artifact Registry - optional)
  ├── gke (GKE Cluster)
  └── api-gateway (API Gateway)
```

## Module Details

### project/

Manages:
- Google Cloud API enablement
- Identity Platform configuration

**Inputs:**
- `project_id`
- `required_apis` (list of APIs to enable)
- `authorized_domains` (for Identity Platform)

**Outputs:**
- `identity_platform_config_id` (used as dependency)

### iam/

Manages:
- Kubernetes service account
- IAM role bindings for:
  - Cloud SQL client
  - Firestore user
  - Identity Platform viewer
  - Storage admin
  - Secret Manager accessor
  - Workload Identity

**Inputs:**
- `project_id`
- `app_name`
- `service_account_name`
- `project_apis_enabled` (dependency)

**Outputs:**
- `service_account_email`
- `service_account_name`

### database/

Manages:
- Secret Manager secret for database password
- Cloud SQL instance
- Cloud SQL database
- Cloud SQL user
- IAM binding for secret access

**Inputs:**
- `project_id`, `region`
- Database configuration (`db_name`, `db_user`, `db_instance_name`, etc.)
- `db_password` (optional - auto-generated if null)
- `service_account_email` (for secret access)
- `deletion_protection`

**Outputs:**
- Database connection details
- Secret ID

### storage/

Manages:
- Cloud Storage bucket
- IAM bindings for storage access

**Inputs:**
- `project_id`
- Storage configuration (`bucket_name`, `bucket_location`, etc.)
- `service_account_email`
- `force_destroy` (boolean)
- `labels`

**Outputs:**
- Bucket name and URL

### firestore/

Manages:
- Firestore database

**Inputs:**
- `project_id`
- `firestore_location`

**Outputs:**
- Firestore database name and location

### artifact-registry/

Manages:
- Artifact Registry repository

**Inputs:**
- `project_id`, `region`
- `app_name`
- `repository_id`
- `labels`

**Outputs:**
- Repository name and URL

### gke/

Manages:
- VPC network
- Subnetwork with secondary IP ranges
- GKE Autopilot cluster

**Inputs:**
- `project_id`, `region`
- `app_name`
- CIDR ranges for subnet, services, and pods
- `deletion_protection`

**Outputs:**
- Cluster details
- Network details
- `kubectl_connect_command`

### api-gateway/

Manages:
- API Gateway API
- API Gateway config (from template)
- API Gateway gateway instance

**Inputs:**
- `project_id`, `region`
- `app_name`
- `service_account_email` (for backend authentication)
- `microservices` (routing configuration)

**Outputs:**
- API Gateway URL and hostname

## State Management

- **Backend**: Google Cloud Storage bucket (configured in `provider.tf`)
- **State Location**: `gs://<bucket>/terraform/state`
- **State Locking**: Enabled automatically with GCS backend
- **Versioning**: Recommended for state bucket

## Secrets Management

### Database Password

1. **Auto-generation** (default): If `db_password` is not provided, a random password is generated and stored in Secret Manager
2. **Manual specification**: Set via:
   - Environment variable: `TF_VAR_db_password`
   - CI/CD secrets: `TF_VAR_DB_PASSWORD`
   - **Never** in `terraform.tfvars`

### Secret Storage

- Database password stored in Google Secret Manager
- Service account has `roles/secretmanager.secretAccessor` on the secret
- Secret is accessible to Kubernetes workloads via Workload Identity

## Resource Naming

Resources can be named with:
- **Fixed names**: Default behavior
- **Random suffix**: Set `use_random_suffix = true`
- **Custom suffix**: Set `resource_suffix = "dev"` or `"prod"`

This affects:
- Service account name
- Secret name
- Database instance name
- Bucket name

## Best Practices

1. **Modularity**: Each module is self-contained with clear inputs/outputs
2. **Dependencies**: Modules declare explicit dependencies via `depends_on` or output references
3. **Reusability**: Modules can be reused across environments
4. **State Isolation**: Use separate state files/prefixes for different environments
5. **Secrets**: Never commit secrets; use environment variables or secret managers

## Environment Strategy

For multiple environments (dev, staging, prod):

1. **Option 1: Separate State Files**
   ```bash
   terraform init -backend-config="prefix=terraform/state/prod"
   ```

2. **Option 2: Workspaces**
   ```bash
   terraform workspace new prod
   terraform workspace select prod
   ```

3. **Option 3: Separate Directories**
   ```
   terraform/
   ├── environments/
   │   ├── dev/
   │   ├── staging/
   │   └── prod/
   └── modules/
   ```

## Importing Existing Resources

If you have existing resources:

1. **Identify the resource** in the module
2. **Import using the full resource path**:
   ```bash
   terraform import module.database.google_sql_database_instance.main PROJECT_ID/INSTANCE_NAME
   ```
3. **Review the plan** and adjust configuration to match imported state
4. **Apply** to sync Terraform state with actual resources

See [SETUP.md](./SETUP.md) for detailed import instructions.

