# Service Account für die VM
resource "google_service_account" "app_sa" {
  account_id   = "cad-travel-app-sa"
  display_name = "CAD Travel App Service Account"
  project      = var.project_id
}

# Cloud SQL Client
resource "google_project_iam_member" "app_sa_sql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.app_sa.email}"
}

# Firestore User
resource "google_project_iam_member" "app_sa_firestore_user" {
  project = var.project_id
  role    = "roles/datastore.user"
  member  = "serviceAccount:${google_service_account.app_sa.email}"
}

# Storage Object Viewer (Lesen)
resource "google_project_iam_member" "app_sa_storage_viewer" {
  project = var.project_id
  role    = "roles/storage.objectViewer"
  member  = "serviceAccount:${google_service_account.app_sa.email}"
}

# Storage Object Creator (Upload) - NEU!
resource "google_project_iam_member" "app_sa_storage_creator" {
  project = var.project_id
  role    = "roles/storage.objectCreator"
  member  = "serviceAccount:${google_service_account.app_sa.email}"
}

# Service Account Token Creator
resource "google_service_account_iam_member" "service_account_token_creator" {
  service_account_id = google_service_account.app_sa.name
  role               = "roles/iam.serviceAccountTokenCreator"
  member             = "serviceAccount:${google_service_account.app_sa.email}"
}

# Service Account Key
resource "google_service_account_key" "app_sa_key" {
  service_account_id = google_service_account.app_sa.name
}
