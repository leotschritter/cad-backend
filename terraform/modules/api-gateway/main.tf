# API Gateway API
resource "google_api_gateway_api" "api_gateway" {
  provider     = google-beta
  api_id       = "${var.app_name}-api"
  display_name = "${var.app_name} API Gateway"
  project      = var.project_id

  depends_on = [var.project_apis_enabled]
}

# API Gateway API Config
resource "google_api_gateway_api_config" "api_config" {
  provider = google-beta
  api      = google_api_gateway_api.api_gateway.api_id

  # Stable based on content, NOT timestamp
  api_config_id = "${var.app_name}-api-config-${substr(sha256(templatefile("${path.module}/api-gateway-config.yaml", {
    app_name            = var.app_name
    services            = var.microservices
    firebase_project_id = var.project_id
  })), 0, 8)}"

  display_name = "${var.app_name} API Config"

  openapi_documents {
    document {
      path = "api-config.yaml"
      contents = base64encode(templatefile("${path.module}/api-gateway-config.yaml", {
        app_name            = var.app_name
        services            = var.microservices
        firebase_project_id = var.project_id
      }))
    }
  }

  gateway_config {
    backend_config {
      google_service_account = var.service_account_email
    }
  }

  lifecycle {
    create_before_destroy = true
  }

  depends_on = [
    google_api_gateway_api.api_gateway,
  ]
}

# API Gateway instance
resource "google_api_gateway_gateway" "api_gateway" {
  provider     = google-beta
  api_config   = google_api_gateway_api_config.api_config.id
  gateway_id   = "${var.app_name}-gateway"
  display_name = "${var.app_name} Gateway"
  region       = var.region
  project      = var.project_id

  depends_on = [
    google_api_gateway_api_config.api_config
  ]
}

