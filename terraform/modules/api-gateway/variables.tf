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

variable "service_account_email" {
  description = "Service account email for API Gateway backend"
  type        = string
}

variable "microservices" {
  description = "Map of microservice configurations for API Gateway routing"
  type = map(object({
    name         = string
    ingress_url  = string
    path_prefix  = string
    service_name = string
    namespace    = string
    port         = number
  }))
}

variable "project_apis_enabled" {
  description = "Dependency on project APIs being enabled"
  type        = any
  default     = []
}

