# Local variables for resource naming
locals {
  suffix               = var.resource_suffix != "" ? var.resource_suffix : (var.use_random_suffix ? random_id.suffix.hex : "")
  db_instance_name     = var.resource_suffix != "" || var.use_random_suffix ? "${var.db_instance_name}-${local.suffix}" : var.db_instance_name
  service_account_name = var.use_random_suffix ? "${var.app_name}-sa-${local.suffix}" : "${var.app_name}-sa"
  secret_name          = var.use_random_suffix ? "${var.app_name}-db-password-${local.suffix}" : "${var.app_name}-db-password"
  bucket_name          = var.use_random_suffix ? "${var.project_id}-${var.bucket_name}-${local.suffix}" : "${var.project_id}-${var.bucket_name}"
}

# Generate random suffix for unique resource names
resource "random_id" "suffix" {
  byte_length = 4
}

# Project Services Module
module "project" {
  source = "./modules/project"

  project_id         = var.project_id
  required_apis      = var.required_apis
  authorized_domains = var.authorized_domains
}

# IAM Module
module "iam" {
  source = "./modules/iam"

  project_id           = var.project_id
  app_name             = var.app_name
  service_account_name = local.service_account_name
  project_apis_enabled = module.project.identity_platform_config_id
}

# Database Module
module "database" {
  source = "./modules/database"

  project_id            = var.project_id
  region                = var.region
  db_name               = var.db_name
  db_user               = var.db_user
  db_instance_name      = local.db_instance_name
  db_tier               = var.db_tier
  db_availability_type  = var.db_availability_type
  disk_size             = var.db_disk_size
  db_password           = var.db_password
  secret_name           = local.secret_name
  service_account_email = module.iam.service_account_email
  deletion_protection   = var.deletion_protection
  project_apis_enabled  = module.project.identity_platform_config_id
}

# Storage Module
module "storage" {
  source = "./modules/storage"

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
  source = "./modules/firestore"

  project_id           = var.project_id
  firestore_location   = var.firestore_location
  project_apis_enabled = module.project.identity_platform_config_id
}

# Artifact Registry Module
module "artifact_registry" {
  count  = var.create_artifact_registry ? 1 : 0
  source = "./modules/artifact-registry"

  project_id           = var.project_id
  region               = var.region
  app_name             = var.app_name
  repository_id        = var.artifact_registry_name
  labels               = var.labels
  project_apis_enabled = module.project.identity_platform_config_id
}

# GKE Module
module "gke" {
  source = "./modules/gke"

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
  source = "./modules/api-gateway"

  project_id            = var.project_id
  region                = var.region
  app_name              = var.app_name
  service_account_email = module.iam.service_account_email
  microservices         = var.microservices
  project_apis_enabled  = module.project.identity_platform_config_id
}

