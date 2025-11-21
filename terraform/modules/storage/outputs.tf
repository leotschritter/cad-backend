output "bucket_name" {
  description = "The name of the Cloud Storage bucket"
  value       = google_storage_bucket.app_bucket.name
}

output "bucket_url" {
  description = "The URL of the Cloud Storage bucket"
  value       = google_storage_bucket.app_bucket.url
}

output "firestore_database_name" {
  description = "The name of the Firestore database"
  value       = google_firestore_database.database.name
}

output "firestore_location" {
  description = "The location of the Firestore database"
  value       = google_firestore_database.database.location_id
}

output "artifact_registry_name" {
  description = "The name of the Artifact Registry repository"
  value       = var.create_artifact_registry ? google_artifact_registry_repository.docker_repo[0].name : null
}

output "artifact_registry_url" {
  description = "The URL of the Artifact Registry repository"
  value       = var.create_artifact_registry ? "${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_name}" : null
}

