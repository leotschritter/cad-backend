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

variable "deletion_protection" {
  description = "Enable deletion protection"
  type        = bool
  default     = false
}

variable "project_apis_enabled" {
  description = "Dependency on project APIs being enabled"
  type        = any
  default     = []
}

