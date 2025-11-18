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

variable "zone" {
  description = "The GCP zone for resources"
  type        = string
  default     = "europe-west1-b"
}

# Application Configuration
variable "app_name" {
  description = "The name of the application"
  type        = string
  default     = "travel-backend"
}

variable "app_version" {
  description = "The version/tag of the application to deploy"
  type        = string
  default     = "latest"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "prod"
}

# Cloud SQL Configuration
variable "db_name" {
  description = "The name of the database"
  type        = string
  default     = "travel-db"
}

variable "db_user" {
  description = "The database user name"
  type        = string
  default     = "cad_db_user"
}

variable "db_instance_name" {
  description = "The name of the Cloud SQL instance"
  type        = string
  default     = "cad-travel-db"
}

variable "db_tier" {
  description = "The machine type for Cloud SQL instance"
  type        = string
  default     = "db-f1-micro" # Change to db-g1-small or higher for production
}

variable "db_availability_type" {
  description = "Availability type for Cloud SQL (ZONAL or REGIONAL)"
  type        = string
  default     = "ZONAL" # Use REGIONAL for high availability
}

variable "db_password" {
  description = "Fixed database password to use for the Cloud SQL user and Secret Manager (provide via terraform.tfvars)."
  type        = string
  sensitive   = true
}

# Cloud Storage Configuration
variable "bucket_name" {
  description = "The name of the Cloud Storage bucket"
  type        = string
  default     = "tripico-images"
}

variable "bucket_location" {
  description = "The location of the Cloud Storage bucket"
  type        = string
  default     = "EU"
}


# Artifact Registry Configuration
variable "artifact_registry_name" {
  description = "The name of the Artifact Registry repository"
  type        = string
  default     = "docker-repo"
}

variable "create_artifact_registry" {
  description = "Whether to create GCP Artifact Registry (required for Kubernetes deployments)"
  type        = bool
  default     = true
}

# Docker Image Configuration
variable "docker_image_url" {
  description = "Full Docker image URL (e.g., ghcr.io/username/travel-backend:latest). If empty, uses GCP Artifact Registry"
  type        = string
  default     = ""
}


# Firestore Configuration
variable "firestore_location" {
  description = "The location for Firestore database"
  type        = string
  default     = "europe-west1"
}


# Tags and Labels
variable "labels" {
  description = "Labels to apply to resources"
  type        = map(string)
  default = {
    app         = "travel-backend"
    managed-by  = "terraform"
    environment = "prod"
  }
}

# Resource Naming
variable "use_random_suffix" {
  description = "Add random suffix to resource names for uniqueness (useful for fresh deployments)"
  type        = bool
  default     = false  # Changed to false - prefer using existing resources
}

variable "resource_suffix" {
  description = "Optional suffix for resource names (leave empty to use random)"
  type        = string
  default     = ""
}

variable "import_existing_resources" {
  description = "Whether to use existing resources if they exist (true) or always create fresh (false)"
  type        = bool
  default     = true  # Prefer using existing resources
}

# Kubernetes/GKE Configuration
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

# Microservices Configuration
variable "microservices" {
  description = "Map of microservice configurations for API Gateway routing"
  type = map(object({
    name         = string
    service_name = string
    namespace    = string
    port         = number
    path_prefix  = string
  }))
  default = {
    comment = {
      name         = "itinerary-service"
      service_name = "itinerary-service"
      namespace    = "default"
      port         = 8080
      path_prefix  = "/comment"
    }
    itinerary = {
      name         = "itinerary-service"
      service_name = "itinerary-service"
      namespace    = "default"
      port         = 8080
      path_prefix  = "/itinerary"
    }
    like = {
      name         = "itinerary-service"
      service_name = "itinerary-service"
      namespace    = "default"
      port         = 8080
      path_prefix  = "/like"
    }
    location = {
      name         = "itinerary-service"
      service_name = "itinerary-service"
      namespace    = "default"
      port         = 8080
      path_prefix  = "/location"
    }
    user = {
      name         = "itinerary-service"
      service_name = "itinerary-service"
      namespace    = "default"
      port         = 8080
      path_prefix  = "/user"
    }
    travel-warnings = {
      name         = "travel-warnings-api"
      service_name = "travel-warnings-api"
      namespace    = "default"
      port         = 8080
      path_prefix  = "/warnings"
    }
  }
}
