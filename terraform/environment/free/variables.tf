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
  default     = "tripico"
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

variable "db_disk_size" {
  description = "Disk size in GB for Cloud SQL instance"
  type        = number
  default     = 10
}

variable "deletion_protection" {
  description = "Enable deletion protection for critical resources"
  type        = bool
  default     = false
}

variable "bucket_force_destroy" {
  description = "Force destroy bucket even if it contains objects"
  type        = bool
  default     = false
}

variable "required_apis" {
  description = "List of Google Cloud APIs to enable"
  type        = list(string)
  default = [
    "sqladmin.googleapis.com",
    "artifactregistry.googleapis.com",
    "firestore.googleapis.com",
    "storage-api.googleapis.com",
    "iamcredentials.googleapis.com",
    "secretmanager.googleapis.com",
    "compute.googleapis.com",
    "container.googleapis.com",
    "apigateway.googleapis.com",
    "servicemanagement.googleapis.com",
    "servicecontrol.googleapis.com",
  ]
}

variable "authorized_domains" {
  description = "List of authorized domains for Identity Platform"
  type        = list(string)
  default = [
    "graphite-plane-474510-s9.firebaseapp.com",
    "graphite-plane-474510-s9.web.app",
    "tripico.fun",
    "frontend.tripico.fun",
    "api.tripico.fun",
    "iaas-476910.firebaseapp.com",
    "iaas-476910.web.app",
  ]
}

variable "db_password" {
  description = "Database password (optional - if not provided, will be auto-generated). Set via environment variable TF_VAR_db_password or CI/CD secrets. NEVER commit to version control."
  type        = string
  sensitive   = true
  default     = null
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
  default     = false # LEAVE 'false' ON main AND 'true' ON develop
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

variable "firebase_project_id" {
  description = "Firebase Auth Project ID"
  type        = string
  default     = "graphite-plane-474510-s9"
}



# Tags and Labels
variable "labels" {
  description = "Labels to apply to resources"
  type        = map(string)
  default = {
    app         = "tripico"
    managed-by  = "terraform"
    environment = "prod"
  }
}

# Resource Naming
variable "use_random_suffix" {
  description = "Add random suffix to resource names for uniqueness (useful for fresh deployments)"
  type        = bool
  default     = false # Changed to false - prefer using existing resources
}

variable "resource_suffix" {
  description = "Optional suffix for resource names (leave empty to use random)"
  type        = string
  default     = ""
}

variable "import_existing_resources" {
  description = "Whether to use existing resources if they exist (true) or always create fresh (false)"
  type        = bool
  default     = true # Prefer using existing resources
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
# Note: This variable defines the routing configuration for API Gateway.
# Each microservice is routed via its ingress URL (not Kubernetes service names).
variable "microservices" {
  description = "Map of microservice configurations for API Gateway routing via ingress URLs"
  type = map(object({
    name        = string # Service name (for reference)
    ingress_url = string # Ingress URL (e.g., https://itinerary.tripico.fun) - used by API Gateway
    path_prefix = string # API Gateway path prefix (e.g., /comment)
    # Legacy fields (kept for reference/documentation, not used in API Gateway routing)
    service_name = string # Kubernetes service name (for reference only)
    namespace    = string # Kubernetes namespace (for reference only)
    port         = number # Service port (for reference only)
  }))
  default = {
    comment = {
      name         = "comment-service"
      ingress_url  = "https://cl-prod.tripico.fun"
      path_prefix  = "/comment"
      service_name = "comment-service"
      namespace    = "default"
      port         = 8080
    }
    itinerary = {
      name         = "itinerary-service"
      ingress_url  = "https://itinerary-prod.tripico.fun"
      path_prefix  = "/itinerary"
      service_name = "itinerary-service"
      namespace    = "default"
      port         = 8080
    }
    like = {
      name         = "like-service"
      ingress_url  = "https://cl-prod.tripico.fun"
      path_prefix  = "/like"
      service_name = "like-service"
      namespace    = "default"
      port         = 8080
    }
    location = {
      name         = "location-service"
      ingress_url  = "https://itinerary-prod.tripico.fun"
      path_prefix  = "/location"
      service_name = "location-service"
      namespace    = "default"
      port         = 8080
    }
    user = {
      name         = "user-service"
      ingress_url  = "https://itinerary-prod.tripico.fun"
      path_prefix  = "/user"
      service_name = "user-service"
      namespace    = "default"
      port         = 8080
    }
    travel-warnings = {
      name         = "travel-warnings-service"
      ingress_url  = "https://warnings-prod.tripico.fun"
      path_prefix  = "/warnings"
      service_name = "travel-warnings-service"
      namespace    = "default"
      port         = 8080
    }
    weather = {
      name         = "weather-service"
      ingress_url  = "https://weather-prod.tripico.fun"
      path_prefix  = "/api/weather"
      service_name = "weather-service"
      namespace    = "default"
      port         = 8080
    }
    feed = {
      name         = "recommendation-feed-service"
      ingress_url  = "https://recommendation-prod.tripico.fun"
      path_prefix  = "/feed"
      service_name = "recommendation-service"
      namespace    = "default"
      port         = 8080
    }
    graph = {
      name         = "recommendation-graph-service"
      ingress_url  = "https://recommendation-prod.tripico.fun"
      path_prefix  = "/graph"
      service_name = "recommendation-service"
      namespace    = "default"
      port         = 8080
    }
  }
}

# DNS Configuration
variable "domain_name" {
  description = "The domain name for the DNS zone"
  type        = string
}

variable "domain_name_prefix" {
  description = "The prefix to add to the domain name for the DNS records"
  type        = string
}