# Example Terraform Variables File
# Copy this file to terraform.tfvars and fill in your values

# Required Variables
project_id = "graphite-plane-474510-s9"

# Optional Variables (defaults are provided)
region      = "europe-west1"
zone        = "europe-west4-a"
app_name    = "tripico"
app_version = "latest"
environment = "prod"

# Cloud SQL Configuration
db_name = "travel-db"
# Use the username that exists in your instance
db_user = "cad_db_user"
# Provide a fixed password (set to a secure value)
db_password          = "h9K#NGBG8j39QpBBti&Q"
db_instance_name     = "cad-travel-db"
db_tier              = "db-f1-micro"
db_availability_type = "ZONAL"

# Resource Management Strategy
use_random_suffix         = false
resource_suffix           = ""
import_existing_resources = true

# Cloud Storage Configuration
bucket_name     = "tripico-images"
bucket_location = "US"

# Artifact Registry Configuration
artifact_registry_name = "docker-repo"

# Docker Image Configuration
create_artifact_registry = false
# Empty -> use Artifact Registry image constructed from variables
docker_image_url = ""

# Domain Configuration (optional)
domain_name = ""

# Firestore Configuration
firestore_location = "europe-west1"

# Labels
labels = {
  app         = "travel-backend"
  managed-by  = "terraform"
  environment = "prod"
  team        = "cad-team"
}
