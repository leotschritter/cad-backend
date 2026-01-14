# =============================================================================
# Enterprise Tier Tenant Configuration - FULLY ISOLATED
# =============================================================================
# This configuration creates a FULLY ISOLATED infrastructure for Enterprise tier.
# Each enterprise tenant gets their own:
# - GKE Cluster (dedicated Kubernetes cluster)
# - VPC Network (isolated networking)
# - Storage Bucket (dedicated object storage)
# - API Gateway (dedicated API endpoint)
# - Static IP for Ingress
# - DNS wildcard record (*.{tenant_name}.{base_domain} - domain derived from DNS zone)
#
# Shared resources (from free tier):
# - IAM Service Account (tripico-sa) - for accessing Firestore/Storage
# - Artifact Registry (for pulling container images)
# - DNS Zone (managed zone, we add records to it)
# - Project APIs (already enabled)
# - Firestore (per-project, tenant isolation via collections)
# =============================================================================

# Local variables for resource naming
locals {
  # Tenant-specific naming - use tenant_name for all resources
  # This creates isolated resources like: tripico-acme-corp-cluster, tripico-acme-corp-network
  tenant_app_name = "${var.app_name}-${var.tenant_name}"
  bucket_name     = "${var.project_id}-${var.bucket_name}-${var.tenant_name}"

  # Use the shared service account from freemium tier (same as standard)
  shared_service_account_name  = "${var.app_name}-sa"
  shared_service_account_email = "${local.shared_service_account_name}@${var.project_id}.iam.gserviceaccount.com"

  # DNS zone name (created by free tier)
  dns_zone_name = "tripico-fun-zone"

  # Base domain derived from environment
  # Prod: tripico.fun, Dev: dev.tripico.fun
  base_domain = var.is_prod_environment ? "tripico.fun" : "dev.tripico.fun"

  # Enterprise tenant subdomain: {tenant_name}.{base_domain}
  # e.g., enterprise-1.dev.tripico.fun or acme-corp.tripico.fun
  tenant_subdomain = "${var.tenant_name}.${local.base_domain}"

  # List of Kubernetes service accounts that need Workload Identity bindings
  # Only services that access GCP resources (Firestore, Storage) need this
  k8s_service_accounts = [
    "itinerary-service-sa",
    "comments-likes-sa"
  ]

  # Build tenant-specific microservices URLs
  # Enterprise tier uses: https://{service}.{tenant_name}.{base_domain}
  # e.g., https://itinerary.enterprise-1.dev.tripico.fun (base_domain derived from DNS zone)
  microservices = {
    comment = {
      name         = "comment-service"
      ingress_url  = "https://cl.${local.tenant_subdomain}"
      path_prefix  = "/comment"
      service_name = "comment-service"
      namespace    = "default"
      port         = 8080
    }
    itinerary = {
      name         = "itinerary-service"
      ingress_url  = "https://itinerary.${local.tenant_subdomain}"
      path_prefix  = "/itinerary"
      service_name = "itinerary-service"
      namespace    = "default"
      port         = 8080
    }
    like = {
      name         = "like-service"
      ingress_url  = "https://cl.${local.tenant_subdomain}"
      path_prefix  = "/like"
      service_name = "like-service"
      namespace    = "default"
      port         = 8080
    }
    location = {
      name         = "location-service"
      ingress_url  = "https://itinerary.${local.tenant_subdomain}"
      path_prefix  = "/location"
      service_name = "location-service"
      namespace    = "default"
      port         = 8080
    }
    user = {
      name         = "user-service"
      ingress_url  = "https://itinerary.${local.tenant_subdomain}"
      path_prefix  = "/user"
      service_name = "user-service"
      namespace    = "default"
      port         = 8080
    }
    # Enterprise tier gets dedicated travel-warnings service
    travel-warnings = {
      name         = "travel-warnings-service"
      ingress_url  = "https://warnings.${local.tenant_subdomain}"
      path_prefix  = "/warnings"
      service_name = "travel-warnings-service"
      namespace    = "default"
      port         = 8080
    }
    # Enterprise tier gets dedicated weather service
    weather = {
      name         = "weather-forecast-service"
      ingress_url  = "https://weather.${local.tenant_subdomain}"
      path_prefix  = "/api/weather"
      service_name = "weather-forecast-service"
      namespace    = "default"
      port         = 8080
    }
    feed = {
      name         = "recommendation-feed-service"
      ingress_url  = "https://recommendation.${local.tenant_subdomain}"
      path_prefix  = "/feed"
      service_name = "recommendation-service"
      namespace    = "default"
      port         = 8080
    }
    graph = {
      name         = "recommendation-graph-service"
      ingress_url  = "https://recommendation.${local.tenant_subdomain}"
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

# Reference the existing shared service account (created by free tier)
# Enterprise shares the GCP service account with freemium/standard for accessing
# Firestore, Storage, and Identity Platform
data "google_service_account" "shared_sa" {
  account_id = local.shared_service_account_name
  project    = var.project_id
}

# =============================================================================
# Identity Platform Configuration
# =============================================================================
# This creates the base Identity Platform config if it doesn't exist.
# The identitytoolkit API is already enabled by free tier.
# If free tier already created this config, Terraform will fail with
# "Identity Platform has already been enabled" - the workflow handles this
# by checking if the API is enabled and attempting import if needed.
# =============================================================================
resource "google_identity_platform_config" "default" {
  provider = google-beta
  project  = var.project_id

  sign_in {
    email {
      enabled           = true
      password_required = true
    }
  }

  lifecycle {
    ignore_changes = all
  }
}

# =============================================================================
# Identity Platform Tenant for User Isolation
# =============================================================================
# Enterprise tier tenants get their own Identity Platform tenant.
# This creates a completely isolated user pool - users registered in one
# enterprise tenant cannot access services of another enterprise or standard tenant.
#
# Freemium users (no tenant) and standard users are NOT allowed to access
# enterprise tier services.
# =============================================================================
resource "google_identity_platform_tenant" "tenant" {
  project = var.project_id

  display_name             = "ent-${var.tenant_name}"
  allow_password_signup    = true
  enable_email_link_signin = true
  disable_auth             = false

  # Ensure Identity Platform config exists before creating tenant
  depends_on = [google_identity_platform_config.default]
}

# Reference the existing DNS zone (created by free tier)
data "google_dns_managed_zone" "main" {
  project = var.project_id
  name    = local.dns_zone_name
}

# Workload Identity bindings for this tenant's cluster
# This allows Kubernetes service accounts in the enterprise cluster to use the shared GCP service account
# Note: Enterprise uses 'default' namespace in its dedicated cluster
resource "google_service_account_iam_member" "workload_identity_bindings" {
  for_each = toset(local.k8s_service_accounts)

  service_account_id = data.google_service_account.shared_sa.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.project_id}.svc.id.goog[default/${each.key}]"
}

# Storage Module - Dedicated bucket for this enterprise tenant
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

# =============================================================================
# Ingress and DNS Configuration
# =============================================================================

# Static IP for this enterprise tenant's ingress controller
resource "google_compute_address" "ingress_ip" {
  project = var.project_id
  name    = "${local.tenant_app_name}-ingress-ip"
  region  = var.region
}

# DNS wildcard record for this enterprise tenant
# Creates: *.{tenant_name}.{base_domain} -> enterprise cluster ingress IP
# e.g., *.enterprise-1.dev.tripico.fun -> 10.x.x.x
resource "google_dns_record_set" "enterprise_wildcard" {
  project      = var.project_id
  managed_zone = data.google_dns_managed_zone.main.name
  name         = "*.${local.tenant_subdomain}."
  type         = "A"
  ttl          = 60

  rrdatas = [google_compute_address.ingress_ip.address]

  depends_on = [google_compute_address.ingress_ip]
}

# NGINX Ingress Controller for this enterprise tenant
resource "helm_release" "ingress_nginx" {
  name             = "ingress-nginx"
  repository       = "https://kubernetes.github.io/ingress-nginx"
  chart            = "ingress-nginx"
  namespace        = "ingress-nginx"
  create_namespace = true
  replace          = true
  force_update     = true

  values = [yamlencode({
    controller = {
      service = {
        type           = "LoadBalancer"
        loadBalancerIP = google_compute_address.ingress_ip.address
      }
    }
  })]

  depends_on = [module.gke, google_compute_address.ingress_ip]
}

# =============================================================================
# API Gateway Module - Dedicated gateway for this enterprise tenant
# =============================================================================
module "api_gateway" {
  source = "../../modules/api-gateway"

  project_id            = var.project_id
  region                = var.region
  app_name              = local.tenant_app_name
  service_account_email = local.shared_service_account_email
  microservices         = local.microservices
}
