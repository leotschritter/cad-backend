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

output "kubectl_connect_command" {
  description = "Command to connect kubectl to the GKE cluster"
  value       = "gcloud container clusters get-credentials ${google_container_cluster.main.name} --region ${var.region} --project ${var.project_id}"
}

output "cluster_ready" {
  description = "Dependency output to ensure cluster is ready before deploying resources"
  value       = google_container_cluster.main.id
}
