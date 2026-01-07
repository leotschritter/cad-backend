# Local variables for resource naming
locals {
  suffix               = var.resource_suffix != "" ? var.resource_suffix : (var.use_random_suffix ? random_id.suffix.hex : "")
  service_account_name = var.use_random_suffix ? "${var.app_name}-sa-${local.suffix}" : "${var.app_name}-sa"
  bucket_name          = var.use_random_suffix ? "${var.project_id}-${var.bucket_name}-${local.suffix}" : "${var.project_id}-${var.bucket_name}"

  # Build tenant-specific microservices URLs
  # For dev: https://itinerary-standard-1.dev.tripico.fun
  # For prod: https://itinerary-standard-1.tripico.fun
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
    travel-warnings = {
      name         = "travel-warnings-service"
      ingress_url  = "https://warnings-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/warnings"
      service_name = "travel-warnings-service"
      namespace    = "default"
      port         = 8080
    }
    weather = {
      name         = "weather-service"
      ingress_url  = "https://weather-${var.tenant_name}.${var.domain_name}"
      path_prefix  = "/api/weather"
      service_name = "weather-service"
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

# Generate random suffix for unique resource names
resource "random_id" "suffix" {
  byte_length = 4
}

# Project Services Module
module "project" {
  source = "../../modules/project"

  project_id         = var.project_id
  required_apis      = var.required_apis
  authorized_domains = var.authorized_domains
}

# IAM Module
module "iam" {
  source = "../../modules/iam"

  project_id           = var.project_id
  app_name             = var.app_name
  service_account_name = local.service_account_name
  project_apis_enabled = module.project.identity_platform_config_id
}

# Storage Module
module "storage" {
  source = "../../modules/storage"

  project_id            = var.project_id
  bucket_name           = local.bucket_name
  bucket_location       = var.bucket_location
  force_destroy         = var.bucket_force_destroy
  service_account_email = module.iam.service_account_email
  labels                = var.labels
  project_apis_enabled  = module.project.identity_platform_config_id
}

# Firestore Module
module "firestore" {
  source = "../../modules/firestore"

  project_id           = var.project_id
  firestore_location   = var.firestore_location
  project_apis_enabled = module.project.identity_platform_config_id
}

# Artifact Registry Module
module "artifact_registry" {
  count  = var.create_artifact_registry ? 1 : 0
  source = "../../modules/artifact-registry"

  project_id           = var.project_id
  region               = var.region
  app_name             = var.app_name
  repository_id        = var.artifact_registry_name
  labels               = var.labels
  project_apis_enabled = module.project.identity_platform_config_id
}

# GKE Module
module "gke" {
  source = "../../modules/gke"

  project_id           = var.project_id
  region               = var.region
  app_name             = var.app_name
  gke_subnet_cidr      = var.gke_subnet_cidr
  gke_services_cidr    = var.gke_services_cidr
  gke_pods_cidr        = var.gke_pods_cidr
  deletion_protection  = var.deletion_protection
  project_apis_enabled = module.project.identity_platform_config_id
}

# API Gateway Module
module "api_gateway" {
  source = "../../modules/api-gateway"

  project_id            = var.project_id
  region                = var.region
  app_name              = var.app_name
  service_account_email = module.iam.service_account_email
  microservices         = local.microservices
  project_apis_enabled  = module.project.identity_platform_config_id
}

module "dns" {
  source = "../../modules/dns"

  project_id           = var.project_id
  region               = var.region
  domain_name          = var.domain_name
  is_prod_environment  = var.is_prod_environment
  project_apis_enabled = module.project.identity_platform_config_id
  gke_cluster_ready    = module.gke.cluster_ready
}