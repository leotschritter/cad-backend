# Enable required Google Cloud APIs
resource "google_project_service" "required_apis" {
  for_each = toset(var.required_apis)

  service            = each.value
  disable_on_destroy = false
}

# Identity Platform Configuration
resource "google_project_service" "identitytoolkit" {
  project = var.project_id
  service = "identitytoolkit.googleapis.com"
}

resource "google_identity_platform_config" "default" {
  provider = google-beta
  project  = var.project_id

  sign_in {
    email {
      enabled           = true
      password_required = true
    }
  }

  authorized_domains = var.authorized_domains

  lifecycle {
    prevent_destroy = true
    ignore_changes  = all
  }

  depends_on = [google_project_service.identitytoolkit]
}

