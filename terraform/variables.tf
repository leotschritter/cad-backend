variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "region" {
  description = "GCP Region"
  type        = string
  default     = "europe-west4"
}

variable "zone" {
  description = "GCP Zone"
  type        = string
  default     = "europe-west4-a"
}

variable "machine_type" {
  description = "VM Machine Type"
  type        = string
  default     = "e2-medium"
}

variable "db_user" {
  description = "PostgreSQL Username"
  type        = string
  default     = "myuser"
}

variable "db_password" {
  description = "PostgreSQL Password"
  type        = string
  sensitive   = true
}

variable "db_name" {
  description = "PostgreSQL Database Name"
  type        = string
  default     = "mydatabase"
}
