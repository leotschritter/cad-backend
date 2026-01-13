# Production environment configuration for Enterprise tier tenants (FULLY ISOLATED)
#
# USAGE:
# The tenant_name and domain_name are dynamically set in CI/CD pipeline.
# This file contains default values for production environment.
#
# Example deployment for enterprise-1:
#   terraform apply -var-file="prod-environment.tfvars" \
#     -var="tenant_name=enterprise-1" \
#     -var="domain_name=tripico.fun"
#
# Example deployment with custom domain (e.g., acme-corp):
#   terraform apply -var-file="prod-environment.tfvars" \
#     -var="tenant_name=acme-corp" \
#     -var="domain_name=acme-corp.tripico.fun"
#
# IMPORTANT: Each enterprise tenant gets:
# - Dedicated GKE cluster (tripico-{tenant_name}-cluster)
# - Dedicated VPC network (tripico-{tenant_name}-network)
# - Dedicated IAM service account (tripico-{tenant_name}-sa)
# - Unique CIDR ranges (must not overlap with other tenants)

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
