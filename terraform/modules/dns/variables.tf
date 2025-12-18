variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "region" {
  description = "The GCP region for resources"
  type        = string
}

variable "ingress_namespace" {
  description = "The namespace where the ingress controller is deployed"
  type        = string
  default     = "ingress-nginx"
}

variable "domain_name" {
  description = "The domain name for the DNS zone"
  type        = string
}

variable "domain_name_prefix" {
  description = "The prefix to add to the domain name for the DNS records"
  type        = string
}

variable "cloud_dns_managed_zone_name" {
  description = "The name of the Cloud DNS managed zone"
  type        = string
  default     = "tripico-fun-zone"
}

variable "is_prod_environment" {
  description = "Flag to indicate if the environment is production"
  type        = bool
  default     = false
}

variable "project_apis_enabled" {
  description = "Dependency on project APIs being enabled"
  type        = any
  default     = []
}
