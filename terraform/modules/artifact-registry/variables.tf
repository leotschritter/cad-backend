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

variable "repository_id" {
  description = "The name of the Artifact Registry repository"
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

