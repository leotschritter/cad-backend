variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "domain_name" {
  description = "The domain name for the DNS zone"
  type        = string
}

variable "region" {
  description = "The GCP region for resources"
  type        = string
  default     = "europe-west1"
}