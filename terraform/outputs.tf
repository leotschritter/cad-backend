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
  value       = module.database.db_instance_name
}

output "db_connection_name" {
  description = "The connection name of the Cloud SQL instance"
  value       = module.database.db_connection_name
}

output "db_name" {
  description = "The name of the database"
  value       = module.database.db_name
}

output "db_user" {
  description = "The database user name"
  value       = module.database.db_user
}

output "db_password_secret_id" {
  description = "The Secret Manager secret ID for the database password"
  value       = module.database.db_password_secret_id
}

# Storage Outputs
output "bucket_name" {
  description = "The name of the Cloud Storage bucket"
  value       = module.storage.bucket_name
}

output "bucket_url" {
  description = "The URL of the Cloud Storage bucket"
  value       = module.storage.bucket_url
}

output "artifact_registry_name" {
  description = "The name of the Artifact Registry repository"
  value       = module.storage.artifact_registry_name
}

output "artifact_registry_url" {
  description = "The URL of the Artifact Registry repository"
  value       = module.storage.artifact_registry_url
}

output "docker_image_url" {
  description = "The full Docker image URL for Kubernetes deployments"
  value       = var.docker_image_url != "" ? var.docker_image_url : "${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_name}/${var.app_name}:${var.app_version}"
}

# Service Account Outputs
output "service_account_email" {
  description = "The email of the Kubernetes service account"
  value       = module.iam.service_account_email
}

output "service_account_name" {
  description = "The name of the Kubernetes service account"
  value       = module.iam.service_account_name
}

# Firestore Outputs
output "firestore_database_name" {
  description = "The name of the Firestore database"
  value       = module.storage.firestore_database_name
}

output "firestore_location" {
  description = "The location of the Firestore database"
  value       = module.storage.firestore_location
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
    ${module.gke.kubectl_connect_command}

    # Update Kubernetes deployment:
    kubectl set image deployment/itinerary-service itinerary-service=${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_name}/${var.app_name}:${var.app_version}
  EOT
}

# Kubernetes/GKE Outputs
output "gke_cluster_name" {
  description = "The name of the GKE cluster"
  value       = module.gke.gke_cluster_name
}

output "gke_cluster_location" {
  description = "The location of the GKE cluster"
  value       = module.gke.gke_cluster_location
}

output "gke_cluster_endpoint" {
  description = "The endpoint of the GKE cluster"
  value       = module.gke.gke_cluster_endpoint
}

output "gke_network_name" {
  description = "The name of the GKE network"
  value       = module.gke.gke_network_name
}

output "gke_subnet_name" {
  description = "The name of the GKE subnetwork"
  value       = module.gke.gke_subnet_name
}

# API Gateway Outputs
output "api_gateway_name" {
  description = "The name of the API Gateway"
  value       = module.api_gateway.api_gateway_name
}

output "api_gateway_gateway_name" {
  description = "The name of the API Gateway gateway"
  value       = module.api_gateway.api_gateway_gateway_name
}

output "api_gateway_url" {
  description = "The URL of the API Gateway"
  value       = module.api_gateway.api_gateway_url
}

output "api_gateway_default_hostname" {
  description = "The default hostname for the API Gateway"
  value       = module.api_gateway.api_gateway_default_hostname
}

# Kubernetes Connection Command
output "kubectl_connect_command" {
  description = "Command to connect kubectl to the GKE cluster"
  value       = module.gke.kubectl_connect_command
}

output "service_urls" {
  description = "Important service URLs"
  value = {
    # API Gateway URLs (for public API access)
    api_gateway = module.api_gateway.api_gateway_url

    # API endpoints via API Gateway
    comment_api   = "${module.api_gateway.api_gateway_url}/comment"
    itinerary_api = "${module.api_gateway.api_gateway_url}/itinerary"
    like_api      = "${module.api_gateway.api_gateway_url}/like"
    location_api  = "${module.api_gateway.api_gateway_url}/location"
    user_api      = "${module.api_gateway.api_gateway_url}/user"
    warnings_api  = "${module.api_gateway.api_gateway_url}/warnings"
  }
}

