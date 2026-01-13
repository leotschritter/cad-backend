# =============================================================================
# Enterprise Tier Variables - FULLY ISOLATED
# =============================================================================
# These variables configure a fully isolated enterprise tenant with:
# - Dedicated GKE cluster
# - Dedicated networking (VPC/subnet)
# - Dedicated IAM service account
# - Dedicated storage bucket
# - Dedicated API Gateway
# - Configurable domain name
# =============================================================================

# Project Configuration
variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "region" {
  description = "The GCP region for resources"
  type        = string
  default     = "europe-west1"
}

# Application Configuration
variable "app_name" {
  description = "The base name of the application (tenant name will be appended)"
  type        = string
  default     = "tripico"
}

# Tenant Configuration
variable "tenant_name" {
  description = "The name of this enterprise tenant (e.g., 'enterprise-1', 'acme-corp')"
  type        = string

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]*[a-z0-9]$", var.tenant_name)) && length(var.tenant_name) <= 30
    error_message = "Tenant name must be lowercase alphanumeric with hyphens, start with a letter, and be at most 30 characters."
  }
}

# Domain Configuration (configurable for enterprise)
variable "domain_name" {
  description = "The domain name for this enterprise tenant (e.g., 'dev.tripico.fun', 'tripico.fun', or custom domain)"
  type        = string
}

# Cloud Storage Configuration
variable "bucket_name" {
  description = "Base name for the Cloud Storage bucket (will be prefixed with project_id and suffixed with tenant_name)"
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

# GKE Configuration
# Each enterprise tenant gets a separate cluster in its own VPC, so CIDR ranges don't conflict
variable "gke_subnet_cidr" {
  description = "CIDR range for GKE subnetwork"
  type        = string
  default     = "10.0.0.0/16"
}

variable "gke_services_cidr" {
  description = "CIDR range for GKE services (secondary IP range)"
  type        = string
  default     = "192.168.0.0/24"
}

variable "gke_pods_cidr" {
  description = "CIDR range for GKE pods (secondary IP range)"
  type        = string
  default     = "192.168.1.0/24"
}

variable "deletion_protection" {
  description = "Enable deletion protection for GKE cluster"
  type        = bool
  default     = false
}

# Tags and Labels
variable "labels" {
  description = "Labels to apply to resources"
  type        = map(string)
  default = {
    app        = "tripico"
    managed-by = "terraform"
    tier       = "enterprise"
  }
}
