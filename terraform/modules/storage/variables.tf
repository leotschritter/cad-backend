variable "project_id" {
  description = "The GCP project ID"
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
