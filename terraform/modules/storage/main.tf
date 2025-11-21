# Cloud Storage Bucket
resource "google_storage_bucket" "app_bucket" {
  name          = var.bucket_name
  location      = var.bucket_location
  force_destroy = var.force_destroy

  uniform_bucket_level_access = true

  cors {
    origin          = ["*"]
    method          = ["GET", "HEAD", "PUT", "POST", "DELETE"]
    response_header = ["*"]
    max_age_seconds = 3600
  }

  labels = var.labels

  depends_on = [var.project_apis_enabled]
}

# IAM Binding - Storage Object Admin
resource "google_storage_bucket_iam_member" "kubernetes_storage_admin" {
  bucket = google_storage_bucket.app_bucket.name
  role   = "roles/storage.objectAdmin"
  member = var.service_account_email
}

# Firestore Database
resource "google_firestore_database" "database" {
  project     = var.project_id
  name        = "(default)"
  location_id = var.firestore_location
  type        = "FIRESTORE_NATIVE"

  depends_on = [var.project_apis_enabled]
}

# Artifact Registry Repository (optional)
resource "google_artifact_registry_repository" "docker_repo" {
  count = var.create_artifact_registry ? 1 : 0

  location      = var.region
  repository_id = var.artifact_registry_name
  description   = "Docker repository for ${var.app_name}"
  format        = "DOCKER"

  labels = var.labels

  depends_on = [var.project_apis_enabled]
}

