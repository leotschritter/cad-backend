output "service_account_email" {
  description = "The email of the Kubernetes service account"
  value       = google_service_account.kubernetes_sa.email
}

output "service_account_name" {
  description = "The name of the Kubernetes service account"
  value       = google_service_account.kubernetes_sa.name
}

