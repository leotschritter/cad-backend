# Service Account for Kubernetes workloads
resource "google_service_account" "kubernetes_sa" {
  account_id   = var.service_account_name
  display_name = "Kubernetes SA for ${var.app_name}"
  description  = "Service account for Kubernetes workloads to access Cloud SQL, Firestore, and Cloud Storage"

  depends_on = [var.project_apis_enabled]
}

# IAM Bindings
resource "google_project_iam_member" "kubernetes_sql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.kubernetes_sa.email}"
}

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

resource "google_service_account_iam_member" "workload_identity_user" {
  service_account_id = google_service_account.kubernetes_sa.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.project_id}.svc.id.goog[default/itinerary-service-sa]"
}

resource "google_project_iam_member" "kubernetes_storage_admin_project" {
  project = var.project_id
  role    = "roles/storage.admin"
  member  = "serviceAccount:${google_service_account.kubernetes_sa.email}"
}

resource "google_service_account_iam_member" "workload_comment_like_user" {
  service_account_id = google_service_account.kubernetes_sa.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.project_id}.svc.id.goog[default/comments-likes-sa]"
}
