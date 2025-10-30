# Output values for the infrastructure

output "project_id" {
  description = "The GCP project ID"
  value       = var.project_id
}

output "region" {
  description = "The GCP region"
  value       = var.region
}

# Cloud Run Outputs
output "cloud_run_url" {
  description = "The URL of the Cloud Run service"
  value       = google_cloud_run_v2_service.main.uri
}

output "cloud_run_service_name" {
  description = "The name of the Cloud Run service"
  value       = google_cloud_run_v2_service.main.name
}

# Cloud SQL Outputs
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

# Storage Outputs
output "bucket_name" {
  description = "The name of the Cloud Storage bucket"
  value       = google_storage_bucket.app_bucket.name
}

output "bucket_url" {
  description = "The URL of the Cloud Storage bucket"
  value       = google_storage_bucket.app_bucket.url
}

# Artifact Registry Outputs (if using GCP Artifact Registry)
output "artifact_registry_name" {
  description = "The name of the Artifact Registry repository"
  value       = var.create_artifact_registry ? google_artifact_registry_repository.docker_repo[0].name : "N/A - Using external registry"
}

output "artifact_registry_url" {
  description = "The URL of the Artifact Registry repository"
  value       = var.create_artifact_registry ? "${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_name}" : "N/A - Using external registry"
}

output "docker_image_url" {
  description = "The full Docker image URL used by Cloud Run"
  value       = var.docker_image_url != "" ? var.docker_image_url : "${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_name}/${var.app_name}:${var.app_version}"
}

# Service Account Outputs
output "service_account_email" {
  description = "The email of the Cloud Run service account"
  value       = google_service_account.cloud_run_sa.email
}

output "service_account_name" {
  description = "The name of the Cloud Run service account"
  value       = google_service_account.cloud_run_sa.name
}

# Firestore Outputs
output "firestore_database_name" {
  description = "The name of the Firestore database"
  value       = google_firestore_database.database.name
}

output "firestore_location" {
  description = "The location of the Firestore database"
  value       = google_firestore_database.database.location_id
}

# Deployment Information
output "deployment_commands" {
  description = "Commands to build and deploy the application"
  value = <<-EOT
    # Authenticate with Artifact Registry:
    gcloud auth configure-docker ${var.region}-docker.pkg.dev

    # Build and push Docker image:
    docker build -t ${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_name}/${var.app_name}:${var.app_version} .
    docker push ${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_name}/${var.app_name}:${var.app_version}

    # Update Cloud Run service with new image:
    gcloud run services update ${var.cloud_run_service_name} \
      --image ${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_name}/${var.app_name}:${var.app_version} \
      --region ${var.region}
  EOT
}

output "service_urls" {
  description = "Important service URLs"
  value = {
    cloud_run   = google_cloud_run_v2_service.main.uri
    swagger_ui  = "${google_cloud_run_v2_service.main.uri}/q/swagger-ui/"
    openapi     = "${google_cloud_run_v2_service.main.uri}/q/openapi"
    health      = "${google_cloud_run_v2_service.main.uri}/q/health"
  }
}

