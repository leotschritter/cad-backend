variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "app_name" {
  description = "The name of the application"
  type        = string
}

variable "service_account_name" {
  description = "The name of the service account"
  type        = string
}

variable "project_apis_enabled" {
  description = "Dependency on project APIs being enabled"
  type        = any
  default     = []
}

