# Dev environment configuration for Enterprise tier tenants (FULLY ISOLATED)
#
# USAGE:
# The tenant_name is set dynamically in CI/CD pipeline.
# The base domain (dev.tripico.fun) is derived automatically from the shared DNS zone.
#
# Example deployment for enterprise-1:
#   terraform apply -var-file="dev-environment.tfvars" -var="tenant_name=enterprise-1"
#
# This will create DNS records like: *.enterprise-1.dev.tripico.fun
#
# IMPORTANT: Each enterprise tenant gets:
# - Dedicated GKE cluster (tripico-{tenant_name}-cluster)
# - Dedicated VPC network (tripico-{tenant_name}-network)
# - Dedicated storage bucket
# - DNS wildcard record (*.{tenant_name}.dev.tripico.fun)

# Project Configuration (dev environment)
project_id = "iaas-476910"
region     = "europe-west1"

# Application name (tenant name will be appended)
app_name = "tripico"

# Storage Configuration
bucket_location      = "EU"
bucket_force_destroy = true

# GKE Configuration
# Each enterprise tenant gets a separate cluster in its own VPC, so CIDR ranges don't conflict
deletion_protection = false

# Labels
labels = {
  app         = "tripico"
  managed-by  = "terraform"
  environment = "dev"
  tier        = "enterprise"
}
