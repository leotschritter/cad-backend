variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "firestore_location" {
  description = "The location for Firestore database"
  type        = string
  default     = "europe-west1"
}

variable "project_apis_enabled" {
  description = "Dependency on project APIs being enabled"
  type        = any
  default     = []
}

