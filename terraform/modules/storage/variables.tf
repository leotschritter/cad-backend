variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "region" {
  description = "The GCP region for resources"
  type        = string
}

variable "app_name" {
  description = "The name of the application"
  type        = string
}

variable "bucket_name" {
  description = "The name of the Cloud Storage bucket"
  type        = string
}

variable "bucket_location" {
  description = "The location of the Cloud Storage bucket"
  type        = string
  default     = "EU"
}

variable "force_destroy" {
  description = "Force destroy bucket even if it contains objects"
  type        = bool
  default     = false
}

variable "firestore_location" {
  description = "The location for Firestore database"
  type        = string
  default     = "europe-west1"
}

variable "create_artifact_registry" {
  description = "Whether to create GCP Artifact Registry"
  type        = bool
  default     = true
}

variable "artifact_registry_name" {
  description = "The name of the Artifact Registry repository"
  type        = string
  default     = "docker-repo"
}

variable "service_account_email" {
  description = "Service account email for storage access"
  type        = string
}

variable "labels" {
  description = "Labels to apply to resources"
  type        = map(string)
  default     = {}
}

variable "project_apis_enabled" {
  description = "Dependency on project APIs being enabled"
  type        = any
  default     = []
}

