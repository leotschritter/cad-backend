variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "region" {
  description = "GCP Region"
  type        = string
  default     = "europe-west3"
}

variable "zone" {
  description = "GCP Zone"
  type        = string
  default     = "europe-west3-a"
}

variable "environment" {
  description = "Deployment Environment (dev, staging, prod)"
  type        = string
  default     = "prod"
}

# Database Variablen
variable "db_instance_name" {
  description = "Name der Cloud SQL Instanz"
  type        = string
  default     = "cad-travel-db"
}

variable "db_name" {
  description = "Database Name"
  type        = string
  default     = "travel_app_db"
}

variable "db_user" {
  description = "Database User"
  type        = string
  default     = "app_user"
}

variable "db_tier" {
  description = "Cloud SQL Instance Tier"
  type        = string
  default     = "db-f1-micro"
}

# VM Variablen
variable "vm_machine_type" {
  description = "Machine Type für die VM"
  type        = string
  default     = "e2-medium"
}

variable "backend_image" {
  description = "Docker Image für das Backend"
  type        = string
  default     = "ghcr.io/leotschritter/cad-backend:latest"
}

variable "frontend_image" {
  description = "Docker Image für das Frontend"
  type        = string
  default     = "ghcr.io/leotschritter/cad-frontend:iaas-latest"
}

# Security
variable "allowed_ssh_ips" {
  description = "Liste der IP-Adressen, die SSH-Zugriff haben"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

# Firestore
variable "firestore_location" {
  description = "Firestore Location (Multi-Region)"
  type        = string
  default     = "eur3" # Europa Multi-Region
}
