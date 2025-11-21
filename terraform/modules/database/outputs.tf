output "db_instance_name" {
  description = "The name of the Cloud SQL instance"
  value       = google_sql_database_instance.main.name
}

output "db_connection_name" {
  description = "The connection name of the Cloud SQL instance"
  value       = google_sql_database_instance.main.connection_name
}

output "db_name" {
  description = "The name of the database"
  value       = google_sql_database.database.name
}

output "db_user" {
  description = "The database user name"
  value       = google_sql_user.user.name
}

output "db_password_secret_id" {
  description = "The Secret Manager secret ID for the database password"
  value       = google_secret_manager_secret.db_password.secret_id
}

