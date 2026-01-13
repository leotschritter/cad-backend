# =============================================================================
# Standard Tier Tenant Configuration
# =============================================================================
# This configuration creates tenant-specific resources for Standard tier.
# It assumes shared infrastructure (GKE, DNS, Artifact Registry, Firestore)
# already exists from the base/free tier deployment.
#
# Standard tier tenants:
# - Get their own API Gateway, Storage bucket, and Service Account
# - Share GKE cluster, DNS zone, Artifact Registry with all tenants
# - Use shared services for Weather and Travel Warnings
# - Get dedicated namespace in GKE for their microservices
# =============================================================================

# Local variables for resource naming
locals {
  # Tenant-specific naming
  tenant_suffix        = var.tenant_name
  service_account_name = "${var.app_name}-${var.tenant_name}-sa"
  bucket_name          = "${var.project_id}-${var.bucket_name}-${var.tenant_name}"
  api_gateway_name     = "${var.app_name}-${var.tenant_name}"

  # Build tenant-specific microservices URLs
  # Tenant-specific services: itinerary, comments-likes, recommendation
  # Shared services: weather, travel-warnings (from shared namespace)
  #
  # For dev: https://itinerary-standard-1.dev.tripico.fun
  # For prod: https://itinerary-standard-1.tripico.fun
  microservices = {
    comment = {
      name         = "comment-service"
      ingress_url  = "https://cl-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/comment"
      service_name = "comment-service"
      namespace    = var.tenant_name
      port         = 8080
    }
    itinerary = {
      name         = "itinerary-service"
      ingress_url  = "https://itinerary-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/itinerary"
      service_name = "itinerary-service"
      namespace    = var.tenant_name
      port         = 8080
    }
    like = {
      name         = "like-service"
      ingress_url  = "https://cl-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/like"
      service_name = "like-service"
      namespace    = var.tenant_name
      port         = 8080
    }
    location = {
      name         = "location-service"
      ingress_url  = "https://itinerary-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/location"
      service_name = "location-service"
      namespace    = var.tenant_name
      port         = 8080
    }
    user = {
      name         = "user-service"
      ingress_url  = "https://itinerary-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/user"
      service_name = "user-service"
      namespace    = var.tenant_name
      port         = 8080
    }
    # Shared services - point to shared namespace URLs
    travel-warnings = {
      name         = "travel-warnings-service"
      ingress_url  = "https://warnings.${var.domain_name}"
      path_prefix  = "/warnings"
      service_name = "travel-warnings-service"
      namespace    = "shared"
      port         = 8080
    }
    weather = {
      name         = "weather-forecast-service"
      ingress_url  = "https://weather.${var.domain_name}"
      path_prefix  = "/api/weather"
      service_name = "weather-forecast-service"
      namespace    = "shared"
      port         = 8080
    }
    feed = {
      name         = "recommendation-feed-service"
      ingress_url  = "https://recommendation-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/feed"
      service_name = "recommendation-service"
      namespace    = var.tenant_name
      port         = 8080
    }
    graph = {
      name         = "recommendation-graph-service"
      ingress_url  = "https://recommendation-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/graph"
      service_name = "recommendation-service"
      namespace    = var.tenant_name
      port         = 8080
    }
  }
}

# =============================================================================
# Tenant-Specific Resources
# =============================================================================
# These resources are created for each Standard tier tenant.
# Shared infrastructure (GKE, DNS, etc.) is NOT created here.
# =============================================================================

# IAM Module - Tenant-specific service account
module "iam" {
  source = "../../modules/iam"

  project_id           = var.project_id
  app_name             = local.api_gateway_name
  service_account_name = local.service_account_name
  tenant_name          = var.tenant_name
}

# Storage Module - Tenant-specific bucket
module "storage" {
  source = "../../modules/storage"

  project_id            = var.project_id
  bucket_name           = local.bucket_name
  bucket_location       = var.bucket_location
  force_destroy         = var.bucket_force_destroy
  service_account_email = module.iam.service_account_email
  labels = merge(var.labels, {
    tenant = var.tenant_name
  })
}

# API Gateway Module - Tenant-specific gateway
module "api_gateway" {
  source = "../../modules/api-gateway"

  project_id            = var.project_id
  region                = var.region
  app_name              = local.api_gateway_name
  service_account_email = module.iam.service_account_email
  microservices         = local.microservices
}