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
