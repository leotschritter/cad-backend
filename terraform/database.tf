# ===================================================
# Cloud SQL PostgreSQL Datenbank
# ===================================================

# Random Password für DB
resource "random_password" "db_password" {
  length  = 16
  special = true
}

# Cloud SQL PostgreSQL Instanz
resource "google_sql_database_instance" "main" {
  name             = var.db_instance_name
  database_version = "POSTGRES_15"
  region           = var.region

  settings {
    tier              = var.db_tier
    availability_type = "ZONAL"
    disk_size         = 20
    disk_type         = "PD_SSD"

    backup_configuration {
      enabled                        = true
      start_time                     = "03:00"
      point_in_time_recovery_enabled = true
    }

    ip_configuration {
      ipv4_enabled = true
      authorized_networks {
        name  = "allow-all"
        value = "0.0.0.0/0"
      }
    }

    database_flags {
      name  = "max_connections"
      value = "100"
    }
  }

  deletion_protection = false

  depends_on = [google_project_service.sql_admin]
}

# PostgreSQL Datenbank
resource "google_sql_database" "database" {
  name     = var.db_name
  instance = google_sql_database_instance.main.name

  deletion_policy = "ABANDON"
}

# PostgreSQL User
resource "google_sql_user" "user" {
  name     = var.db_user
  instance = google_sql_database_instance.main.name
  password = random_password.db_password.result

  deletion_policy = "ABANDON"
}

# Outputs
output "db_connection_string" {
  description = "Cloud SQL Connection String"
  value       = "postgresql://${var.db_user}:${random_password.db_password.result}@${google_sql_database_instance.main.public_ip_address}:5432/${var.db_name}"
  sensitive   = true
}

output "db_public_ip" {
  description = "Cloud SQL Public IP"
  value       = google_sql_database_instance.main.public_ip_address
}

