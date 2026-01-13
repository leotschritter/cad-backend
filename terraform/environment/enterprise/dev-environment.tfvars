# Dev environment configuration for Enterprise tier tenants (FULLY ISOLATED)
#
# USAGE:
# The tenant_name and domain_name are dynamically set in CI/CD pipeline.
# This file contains default values for development environment.
#
# Example deployment for enterprise-1:
#   terraform apply -var-file="dev-environment.tfvars" \
#     -var="tenant_name=enterprise-1" \
#     -var="domain_name=dev.tripico.fun"
#
# Example deployment with custom domain (e.g., acme-corp):
#   terraform apply -var-file="dev-environment.tfvars" \
#     -var="tenant_name=acme-corp" \
#     -var="domain_name=acme-corp.dev.tripico.fun"
#
# IMPORTANT: Each enterprise tenant gets:
# - Dedicated GKE cluster (tripico-{tenant_name}-cluster)
# - Dedicated VPC network (tripico-{tenant_name}-network)
# - Dedicated IAM service account (tripico-{tenant_name}-sa)
# - Unique CIDR ranges (must not overlap with other tenants)

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
