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
  default     = "traveldb"
}

variable "db_user" {
  description = "The database user name"
  type        = string
  default     = "traveluser"
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

# Cloud Run Configuration
variable "cloud_run_service_name" {
  description = "The name of the Cloud Run service"
  type        = string
  default     = "travel-backend"
}

variable "cloud_run_cpu" {
  description = "CPU allocation for Cloud Run service"
  type        = string
  default     = "1"
}

variable "cloud_run_memory" {
  description = "Memory allocation for Cloud Run service"
  type        = string
  default     = "512Mi"
}

variable "cloud_run_max_instances" {
  description = "Maximum number of Cloud Run instances"
  type        = number
  default     = 10
}

variable "cloud_run_min_instances" {
  description = "Minimum number of Cloud Run instances"
  type        = number
  default     = 0
}

variable "cloud_run_timeout" {
  description = "Request timeout for Cloud Run service in seconds"
  type        = number
  default     = 300
}

# Artifact Registry Configuration
variable "artifact_registry_name" {
  description = "The name of the Artifact Registry repository"
  type        = string
  default     = "docker-repo"
}

variable "create_artifact_registry" {
  description = "Whether to create GCP Artifact Registry (set false if using external registry like GitHub)"
  type        = bool
  default     = true
}

# Docker Image Configuration
variable "docker_image_url" {
  description = "Full Docker image URL (e.g., ghcr.io/username/travel-backend:latest). If empty, uses GCP Artifact Registry"
  type        = string
  default     = ""
}

# Domain Configuration (Optional)
variable "domain_name" {
  description = "Custom domain name for the application (e.g., api.tripico.fun)"
  type        = string
  default     = ""
}

# Firestore Configuration
variable "firestore_location" {
  description = "The location for Firestore database"
  type        = string
  default     = "europe-west1"
}

# Networking Configuration
variable "allow_unauthenticated" {
  description = "Allow unauthenticated access to Cloud Run service"
  type        = bool
  default     = true
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

