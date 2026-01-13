# =============================================================================
# Enterprise Tier Tenant Configuration - FULLY ISOLATED
# =============================================================================
# This configuration creates a FULLY ISOLATED infrastructure for Enterprise tier.
# Each enterprise tenant gets their own:
# - GKE Cluster (dedicated Kubernetes cluster)
# - VPC Network (isolated networking)
# - IAM Service Account (dedicated identity)
# - Storage Bucket (dedicated object storage)
# - API Gateway (dedicated API endpoint)
#
# Shared resources (from free tier, read-only access):
# - Artifact Registry (for pulling container images)
# - DNS Zone (for domain records)
# - Project APIs (already enabled)
# - Firestore (per-project, tenant isolation via collections)
# =============================================================================

# Local variables for resource naming
locals {
  # Tenant-specific naming - use tenant_name for all resources
  # This creates isolated resources like: tripico-acme-corp-cluster, tripico-acme-corp-network
  tenant_app_name      = "${var.app_name}-${var.tenant_name}"
  service_account_name = "${var.app_name}-${var.tenant_name}-sa"
  bucket_name          = "${var.project_id}-${var.bucket_name}-${var.tenant_name}"

  # List of Kubernetes service accounts that need Workload Identity bindings
  # Enterprise tier has ALL services (no shared services)
  k8s_service_accounts = [
    "itinerary-service-sa",
    "comments-likes-sa",
    "recommendation-service-sa",
    "travel-warnings-sa",
    "weather-forecast-sa"
  ]

  # Build tenant-specific microservices URLs
  # Enterprise tier has ALL services dedicated (no shared services)
  # Uses configurable domain: https://{service}-{tenant_name}.{domain_name}
  microservices = {
    comment = {
      name         = "comment-service"
      ingress_url  = "https://cl-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/comment"
      service_name = "comment-service"
      namespace    = "default"
      port         = 8080
    }
    itinerary = {
      name         = "itinerary-service"
      ingress_url  = "https://itinerary-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/itinerary"
      service_name = "itinerary-service"
      namespace    = "default"
      port         = 8080
    }
    like = {
      name         = "like-service"
      ingress_url  = "https://cl-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/like"
      service_name = "like-service"
      namespace    = "default"
      port         = 8080
    }
    location = {
      name         = "location-service"
      ingress_url  = "https://itinerary-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/location"
      service_name = "location-service"
      namespace    = "default"
      port         = 8080
    }
    user = {
      name         = "user-service"
      ingress_url  = "https://itinerary-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/user"
      service_name = "user-service"
      namespace    = "default"
      port         = 8080
    }
    # Enterprise tier gets dedicated travel-warnings service
    travel-warnings = {
      name         = "travel-warnings-service"
      ingress_url  = "https://warnings-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/warnings"
      service_name = "travel-warnings-service"
      namespace    = "default"
      port         = 8080
    }
    # Enterprise tier gets dedicated weather service
    weather = {
      name         = "weather-forecast-service"
      ingress_url  = "https://weather-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/api/weather"
      service_name = "weather-forecast-service"
      namespace    = "default"
      port         = 8080
    }
    feed = {
      name         = "recommendation-feed-service"
      ingress_url  = "https://recommendation-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/feed"
      service_name = "recommendation-service"
      namespace    = "default"
      port         = 8080
    }
    graph = {
      name         = "recommendation-graph-service"
      ingress_url  = "https://recommendation-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/graph"
      service_name = "recommendation-service"
      namespace    = "default"
      port         = 8080
    }
  }
}

# =============================================================================
# Fully Isolated Enterprise Resources
# =============================================================================

# IAM Module - Dedicated service account for this enterprise tenant
module "iam" {
  source = "../../modules/iam"

  project_id           = var.project_id
  app_name             = local.tenant_app_name
  service_account_name = local.service_account_name
  tenant_name          = "default" # Enterprise uses 'default' namespace in its own cluster
}

# Storage Module - Dedicated bucket for this enterprise tenant
module "storage" {
  source = "../../modules/storage"

  project_id            = var.project_id
  bucket_name           = local.bucket_name
  bucket_location       = var.bucket_location
  force_destroy         = var.bucket_force_destroy
  service_account_email = module.iam.service_account_email
  labels = merge(var.labels, {
    tenant = var.tenant_name
    tier   = "enterprise"
  })
}

# GKE Module - Dedicated Kubernetes cluster for this enterprise tenant
# Cluster name will be: tripico-{tenant_name}-cluster (e.g., tripico-acme-corp-cluster)
module "gke" {
  source = "../../modules/gke"

  project_id          = var.project_id
  region              = var.region
  app_name            = local.tenant_app_name
  gke_subnet_cidr     = var.gke_subnet_cidr
  gke_services_cidr   = var.gke_services_cidr
  gke_pods_cidr       = var.gke_pods_cidr
  deletion_protection = var.deletion_protection
}

# API Gateway Module - Dedicated gateway for this enterprise tenant
module "api_gateway" {
  source = "../../modules/api-gateway"

  project_id            = var.project_id
  region                = var.region
  app_name              = local.tenant_app_name
  service_account_email = module.iam.service_account_email
  microservices         = local.microservices
}

# =============================================================================
# Outputs for deployment workflows
# =============================================================================
# The outputs.tf file contains all the outputs needed for Helm deployments
# and other downstream processes.
