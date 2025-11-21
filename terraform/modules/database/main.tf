terraform {
  required_providers {
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
  }
}

# Generate random password if not provided
resource "random_password" "db_password" {
  count   = var.db_password == null ? 1 : 0
  length  = 32
  special = true
}

locals {
  db_password_value = var.db_password != null ? var.db_password : random_password.db_password[0].result
}

# Store database password in Secret Manager
resource "google_secret_manager_secret" "db_password" {
  secret_id = var.secret_name

  replication {
    auto {}
  }

  depends_on = [var.project_apis_enabled]
}

resource "google_secret_manager_secret_version" "db_password" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = local.db_password_value
}

# IAM Binding - Secret Manager Secret Accessor
resource "google_secret_manager_secret_iam_member" "kubernetes_secret_accessor" {
  secret_id = google_secret_manager_secret.db_password.id
  role      = "roles/secretmanager.secretAccessor"
  member    = var.service_account_email
}

# Cloud SQL Instance
resource "google_sql_database_instance" "main" {
  name             = var.db_instance_name
  database_version = "POSTGRES_16"
  region           = var.region

  settings {
    tier              = var.db_tier
    availability_type = var.db_availability_type
    disk_size         = var.disk_size
    disk_type         = "PD_SSD"

    backup_configuration {
      enabled                        = true
      start_time                     = "03:00"
      point_in_time_recovery_enabled = false
    }

    ip_configuration {
      ipv4_enabled    = true
      private_network = null
    }

    database_flags {
      name  = "max_connections"
      value = "100"
    }

    insights_config {
      query_insights_enabled  = true
      query_plans_per_minute  = 5
      query_string_length     = 1024
      record_application_tags = true
    }
  }

  deletion_protection = var.deletion_protection

  depends_on = [var.project_apis_enabled]
}

# Cloud SQL Database
resource "google_sql_database" "database" {
  name     = var.db_name
  instance = google_sql_database_instance.main.name
}

# Cloud SQL User
resource "google_sql_user" "user" {
  name     = var.db_user
  instance = google_sql_database_instance.main.name
  password = local.db_password_value

  depends_on = [google_secret_manager_secret_version.db_password]
}

