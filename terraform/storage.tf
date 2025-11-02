# ===================================================
# Cloud Storage Bucket für Bilder
# ===================================================

# Storage Bucket
resource "google_storage_bucket" "images" {
  name          = "${var.project_id}-travel-images"
  location      = var.region
  storage_class = "STANDARD"
  force_destroy = true

  uniform_bucket_level_access = true


  cors {
    origin          = ["*"]
    method          = ["GET", "HEAD", "PUT", "POST", "DELETE"]
    response_header = ["*"]
    max_age_seconds = 3600
  }

  versioning {
    enabled = false
  }

  depends_on = [google_project_service.storage]
}

# Bucket IAM - Öffentlicher Lesezugriff
resource "google_storage_bucket_iam_member" "public_read" {
  bucket = google_storage_bucket.images.name
  role   = "roles/storage.objectViewer"
  member = "allUsers"

  depends_on = [google_storage_bucket.images]
}

# Bucket IAM - Service Account kann schreiben
resource "google_storage_bucket_iam_member" "app_sa_writer" {
  bucket = google_storage_bucket.images.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.app_sa.email}"

  depends_on = [google_storage_bucket.images]
}

# Output
output "storage_bucket_name" {
  description = "Storage Bucket Name"
  value       = google_storage_bucket.images.name
}

output "storage_bucket_url" {
  description = "Storage Bucket URL"
  value       = "gs://${google_storage_bucket.images.name}"
}

