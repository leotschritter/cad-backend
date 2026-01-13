# =============================================================================
# Enterprise Tier Tenant Configuration
# =============================================================================
# This configuration creates tenant-specific resources for Enterprise tier.
# It assumes shared infrastructure (GKE, DNS, Artifact Registry, Firestore, IAM)
# already exists from the base/free tier deployment.
#
# Enterprise tier tenants:
# - Get their own API Gateway with configurable domain
# - Get their own Storage bucket for images
# - SHARE the GCP service account with freemium (tripico-sa)
# - Share GKE cluster, DNS zone, Artifact Registry with all tenants
# - Use shared Firestore (tenant isolation via collections/prefixes)
# - Get dedicated namespace in GKE for their microservices
# - Have full control over their services (no shared services)
# =============================================================================

# Local variables for resource naming
locals {
  # Tenant-specific naming
  tenant_suffix    = var.tenant_name
  bucket_name      = "${var.project_id}-${var.bucket_name}-${var.tenant_name}"
  api_gateway_name = "${var.app_name}-${var.tenant_name}"

  # Use the shared service account from freemium tier
  # This is the service account created by the free tier Terraform
  shared_service_account_name  = "${var.app_name}-sa"
  shared_service_account_email = "${local.shared_service_account_name}@${var.project_id}.iam.gserviceaccount.com"

  # List of Kubernetes service accounts that need Workload Identity bindings
  # These match the serviceAccount.name in the Helm values files
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
    # Enterprise tier gets dedicated travel-warnings service (not shared)
    travel-warnings = {
      name         = "travel-warnings-service"
      ingress_url  = "https://warnings-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/warnings"
      service_name = "travel-warnings-service"
      namespace    = var.tenant_name
      port         = 8080
    }
    # Enterprise tier gets dedicated weather service (not shared)
    weather = {
      name         = "weather-forecast-service"
      ingress_url  = "https://weather-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/api/weather"
      service_name = "weather-forecast-service"
      namespace    = var.tenant_name
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
# These resources are created for each Enterprise tier tenant.
# Shared infrastructure (GKE, DNS, etc.) is NOT created here.
# =============================================================================

# Reference the existing shared service account (created by free tier)
data "google_service_account" "shared_sa" {
  account_id = local.shared_service_account_name
  project    = var.project_id
}

# Workload Identity bindings for this tenant's namespace
# This allows Kubernetes service accounts in the tenant's namespace to use the shared GCP service account
resource "google_service_account_iam_member" "workload_identity_bindings" {
  for_each = toset(local.k8s_service_accounts)

  service_account_id = data.google_service_account.shared_sa.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.project_id}.svc.id.goog[${var.tenant_name}/${each.key}]"
}

# Storage Module - Tenant-specific bucket
module "storage" {
  source = "../../modules/storage"

  project_id            = var.project_id
  bucket_name           = local.bucket_name
  bucket_location       = var.bucket_location
  force_destroy         = var.bucket_force_destroy
  service_account_email = local.shared_service_account_email
  labels = merge(var.labels, {
    tenant = var.tenant_name
    tier   = "enterprise"
  })
}

# API Gateway Module - Tenant-specific gateway with configurable domain
module "api_gateway" {
  source = "../../modules/api-gateway"

  project_id            = var.project_id
  region                = var.region
  app_name              = local.api_gateway_name
  service_account_email = local.shared_service_account_email
  microservices         = local.microservices
}
