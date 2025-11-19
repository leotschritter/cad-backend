# Output values for the infrastructure

output "project_id" {
  description = "The GCP project ID"
  value       = var.project_id
}

output "region" {
  description = "The GCP region"
  value       = var.region
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
  description = "The full Docker image URL for Kubernetes deployments"
  value       = var.docker_image_url != "" ? var.docker_image_url : "${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_name}/${var.app_name}:${var.app_version}"
}

# Service Account Outputs
output "service_account_email" {
  description = "The email of the Kubernetes service account"
  value       = google_service_account.kubernetes_sa.email
}

output "service_account_name" {
  description = "The name of the Kubernetes service account"
  value       = google_service_account.kubernetes_sa.name
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

# Resource naming outputs
output "resource_suffix" {
  description = "The suffix used for resource names"
  value       = local.suffix
}

output "actual_db_instance_name" {
  description = "The actual Cloud SQL instance name with suffix"
  value       = local.db_instance_name
}

output "service_account_id" {
  description = "The service account ID with suffix"
  value       = local.service_account_name
}

output "secret_name" {
  description = "The secret name with suffix"
  value       = local.secret_name
}

output "actual_bucket_name" {
  description = "The actual bucket name with suffix"
  value       = local.bucket_name
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

    # Connect to GKE cluster:
    gcloud container clusters get-credentials ${google_container_cluster.main.name} --region ${var.region} --project ${var.project_id}

    # Update Kubernetes deployment:
    kubectl set image deployment/itinerary-service itinerary-service=${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_name}/${var.app_name}:${var.app_version}
  EOT
}

# Kubernetes/GKE Outputs
output "gke_cluster_name" {
  description = "The name of the GKE cluster"
  value       = google_container_cluster.main.name
}

output "gke_cluster_location" {
  description = "The location of the GKE cluster"
  value       = google_container_cluster.main.location
}

output "gke_cluster_endpoint" {
  description = "The endpoint of the GKE cluster"
  value       = google_container_cluster.main.endpoint
}

output "gke_network_name" {
  description = "The name of the GKE network"
  value       = google_compute_network.gke_network.name
}

output "gke_subnet_name" {
  description = "The name of the GKE subnetwork"
  value       = google_compute_subnetwork.gke_subnet.name
}

# API Gateway Outputs
output "api_gateway_name" {
  description = "The name of the API Gateway"
  value       = google_api_gateway_api.api_gateway.api_id
}

output "api_gateway_gateway_name" {
  description = "The name of the API Gateway gateway"
  value       = google_api_gateway_gateway.api_gateway.gateway_id
}

output "api_gateway_url" {
  description = "The URL of the API Gateway"
  value       = "https://${google_api_gateway_gateway.api_gateway.default_hostname}"
}

output "api_gateway_default_hostname" {
  description = "The default hostname for the API Gateway"
  value       = google_api_gateway_gateway.api_gateway.default_hostname
}



# Kubernetes Connection Command
output "kubectl_connect_command" {
  description = "Command to connect kubectl to the GKE cluster"
  value       = "gcloud container clusters get-credentials ${google_container_cluster.main.name} --region ${var.region} --project ${var.project_id}"
}

output "service_urls" {
  description = "Important service URLs"
  value = {
    # API Gateway URLs (for public API access)
    api_gateway = "https://${google_api_gateway_gateway.api_gateway.default_hostname}"

    # API endpoints via API Gateway
    comment_api   = "https://${google_api_gateway_gateway.api_gateway.default_hostname}/comment"
    itinerary_api = "https://${google_api_gateway_gateway.api_gateway.default_hostname}/itinerary"
    like_api      = "https://${google_api_gateway_gateway.api_gateway.default_hostname}/like"
    location_api  = "https://${google_api_gateway_gateway.api_gateway.default_hostname}/location"
    user_api      = "https://${google_api_gateway_gateway.api_gateway.default_hostname}/user"
    warnings_api  = "https://${google_api_gateway_gateway.api_gateway.default_hostname}/warnings"

  }
}

