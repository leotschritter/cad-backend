# =============================================================================
# Standard Tier Tenant Variables
# =============================================================================
# Variables for tenant-specific resources only.
# Shared infrastructure variables (GKE, DNS, etc.) are NOT needed here.
# =============================================================================

# Project Configuration
variable "project_id" {
  description = "The GCP project ID (shared across all tenants)"
  type        = string
}

variable "region" {
  description = "The GCP region for resources"
  type        = string
  default     = "europe-west1"
}

# Application Configuration
variable "app_name" {
  description = "The base name of the application"
  type        = string
  default     = "tripico"
}

variable "tenant_name" {
  description = "Tenant name for multi-tenant deployments (e.g., standard-1, standard-2)"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "prod"
}

# Cloud Storage Configuration
variable "bucket_name" {
  description = "The base name of the Cloud Storage bucket (tenant name will be appended)"
  type        = string
  default     = "tripico-images"
}

variable "bucket_location" {
  description = "The location of the Cloud Storage bucket"
  type        = string
  default     = "EU"
}

variable "bucket_force_destroy" {
  description = "Force destroy bucket even if it contains objects"
  type        = bool
  default     = false
}

# Tags and Labels
variable "labels" {
  description = "Labels to apply to resources"
  type        = map(string)
  default = {
    app         = "tripico"
    managed-by  = "terraform"
    environment = "prod"
    tier        = "standard"
  }
}

# DNS Configuration
variable "domain_name" {
  description = "The domain name for service URLs (e.g., dev.tripico.fun or tripico.fun)"
  type        = string
}
