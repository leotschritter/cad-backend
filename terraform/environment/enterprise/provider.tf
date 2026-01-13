# =============================================================================
# Enterprise Tier Terraform Configuration - FULLY ISOLATED
# =============================================================================
# This configuration creates fully isolated infrastructure including:
# - Dedicated GKE cluster
# - Dedicated VPC network
# - Dedicated IAM service account
# =============================================================================

terraform {
  required_version = ">= 1.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.38"
    }
    google-beta = {
      source  = "hashicorp/google-beta"
      version = "~> 6.38"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.13"
    }
  }

  # Remote backend configuration
  # Each enterprise tenant gets isolated state via unique prefix
  # The actual prefix is set dynamically in CI/CD (see backend-config-dev.hcl)
  backend "gcs" {
    bucket = "" # Set via backend-config.hcl or -backend-config flag
    prefix = "terraform/state"
  }
}

provider "google" {
  project               = var.project_id
  region                = var.region
  user_project_override = true
  billing_project       = var.project_id
}

provider "google-beta" {
  project               = var.project_id
  region                = var.region
  user_project_override = true
  billing_project       = var.project_id
}

provider "helm" {
  kubernetes {
    config_path = pathexpand("~/.kube/config")
  }
}
