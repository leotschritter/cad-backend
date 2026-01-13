# Service Account for Kubernetes workloads
resource "google_service_account" "kubernetes_sa" {
  account_id   = var.service_account_name
  display_name = "Kubernetes SA for ${var.app_name}"
  description  = "Service account for Kubernetes workloads to access Firestore and Cloud Storage"

  depends_on = [var.project_apis_enabled]
}

# IAM Bindings
resource "google_project_iam_member" "kubernetes_firestore_user" {
  project = var.project_id
  role    = "roles/datastore.user"
  member  = "serviceAccount:${google_service_account.kubernetes_sa.email}"
}

resource "google_project_iam_member" "kubernetes_identity_platform_viewer" {
  project = var.project_id
  role    = "roles/identityplatform.viewer"
  member  = "serviceAccount:${google_service_account.kubernetes_sa.email}"
}

resource "google_service_account_iam_member" "kubernetes_token_creator" {
  service_account_id = google_service_account.kubernetes_sa.name
  role               = "roles/iam.serviceAccountTokenCreator"
  member             = "serviceAccount:${google_service_account.kubernetes_sa.email}"
}

resource "google_project_iam_member" "kubernetes_storage_admin_project" {
  project = var.project_id
  role    = "roles/storage.admin"
  member  = "serviceAccount:${google_service_account.kubernetes_sa.email}"
}

# Workload Identity bindings for service accounts in tenant namespace
# This allows Kubernetes service accounts in the tenant's namespace to use the GCP service account
locals {
  # List of service accounts that need Workload Identity bindings in this tenant's namespace
  # Only services that access external GCP services (Firestore, Storage, Identity Platform)
  service_accounts = [
    "itinerary-service-sa",
    "comments-likes-sa"
  ]
}

resource "google_service_account_iam_member" "workload_identity_bindings" {
  for_each = toset(local.service_accounts)

  service_account_id = google_service_account.kubernetes_sa.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.project_id}.svc.id.goog[${var.tenant_name}/${each.key}]"
}
