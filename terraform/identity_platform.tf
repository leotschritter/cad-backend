// identity_platform.tf
// Aktiviert Identity Toolkit API und legt die Identity Platform Konfiguration an.
// Anpassungen: passe die authorized_domains variable an dein Setup an oder überschreibe sie per terraform.tfvars.

resource "google_project_service" "identitytoolkit" {
  project = var.project_id
  service = "identitytoolkit.googleapis.com"
}

resource "google_identity_platform_config" "default" {
  project = var.project_id

  sign_in {
    email {
      enabled           = true
      password_required = true
    }
  }

  authorized_domains = var.authorized_domains

  depends_on = [google_project_service.identitytoolkit]
}

variable "authorized_domains" {
  description = "Liste der autorisierten Domains für Identity Platform / Firebase Auth (z.B. webapp-urIs)"
  type        = list(string)
  default = [
    "tripico.duckdns.org"
  ]
}


