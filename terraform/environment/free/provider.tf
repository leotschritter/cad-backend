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
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.13"
    }
  }

  # Remote backend configuration
  # The bucket must be created manually before first use (see SETUP.md)
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
