# Production environment configuration for Enterprise tier tenants (FULLY ISOLATED)
#
# USAGE:
# The tenant_name is set dynamically in CI/CD pipeline.
# The base domain (tripico.fun) is derived automatically from the shared DNS zone.
#
# Example deployment for enterprise-1:
#   terraform apply -var-file="prod-environment.tfvars" -var="tenant_name=enterprise-1"
#
# This will create DNS records like: *.enterprise-1.tripico.fun
#
# IMPORTANT: Each enterprise tenant gets:
# - Dedicated GKE cluster (tripico-{tenant_name}-cluster)
# - Dedicated VPC network (tripico-{tenant_name}-network)
# - Dedicated storage bucket
# - DNS wildcard record (*.{tenant_name}.tripico.fun)

# Project Configuration (prod environment)
project_id = "graphite-plane-474510-s9"
region     = "europe-west1"

# Application name (tenant name will be appended)
app_name = "tripico"

# Storage Configuration
bucket_location      = "EU"
bucket_force_destroy = false

# GKE Configuration
# Each enterprise tenant gets a separate cluster in its own VPC, so CIDR ranges don't conflict
deletion_protection = true

# Labels
labels = {
  app         = "tripico"
  managed-by  = "terraform"
  environment = "prod"
  tier        = "enterprise"
}
