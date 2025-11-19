# Generate random suffix for unique resource names
resource "random_id" "suffix" {
  byte_length = 4
}

# Local variables for resource naming
locals {
  suffix = var.resource_suffix != "" ? var.resource_suffix : (var.use_random_suffix ? random_id.suffix.hex : "")
  db_instance_name = var.resource_suffix != "" || var.use_random_suffix ? "${var.db_instance_name}-${local.suffix}" : var.db_instance_name
  service_account_name = var.use_random_suffix ? "${var.app_name}-sa-${local.suffix}" : "${var.app_name}-sa"
  secret_name = var.use_random_suffix ? "${var.app_name}-db-password-${local.suffix}" : "${var.app_name}-db-password"
  bucket_name = var.use_random_suffix ? "${var.project_id}-${var.bucket_name}-${local.suffix}" : "${var.project_id}-${var.bucket_name}"
}

# Enable required Google Cloud APIs
resource "google_project_service" "required_apis" {
  for_each = toset([
    "sqladmin.googleapis.com",
    "artifactregistry.googleapis.com",
    "firestore.googleapis.com",
    "storage-api.googleapis.com",
    "iamcredentials.googleapis.com",
    "secretmanager.googleapis.com",
    "compute.googleapis.com",
    "container.googleapis.com",
    "apigateway.googleapis.com",
    "servicemanagement.googleapis.com",
    "servicecontrol.googleapis.com",
  ])

  service            = each.value
  disable_on_destroy = false
}

# Service Account for Kubernetes workloads
resource "google_service_account" "kubernetes_sa" {
  account_id   = local.service_account_name
  display_name = "Kubernetes SA for ${var.app_name}"
  description  = "Service account for Kubernetes workloads to access Cloud SQL, Firestore, and Cloud Storage"

  depends_on = [google_project_service.required_apis]

  lifecycle {
    prevent_destroy = false
    # If resource exists, import it instead of failing
    # Run: terraform import google_service_account.kubernetes_sa projects/PROJECT_ID/serviceAccounts/SERVICE_ACCOUNT_EMAIL
  }
}

# IAM Binding - Cloud SQL Client
resource "google_project_iam_member" "kubernetes_sql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.kubernetes_sa.email}"
}

# IAM Binding - Firestore User
resource "google_project_iam_member" "kubernetes_firestore_user" {
  project = var.project_id
  role    = "roles/datastore.user"
  member  = "serviceAccount:${google_service_account.kubernetes_sa.email}"
}

# IAM Binding - Identity Platform Token Verifier
resource "google_project_iam_member" "kubernetes_identity_platform_viewer" {
  project = var.project_id
  role    = "roles/identityplatform.viewer"
  member  = "serviceAccount:${google_service_account.kubernetes_sa.email}"
}

# IAM Binding - Service Account Token Creator
resource "google_service_account_iam_member" "kubernetes_token_creator" {
  service_account_id = google_service_account.kubernetes_sa.name
  role               = "roles/iam.serviceAccountTokenCreator"
  member             = "serviceAccount:${google_service_account.kubernetes_sa.email}"
}

# Workload Identity Binding for GKE
resource "google_service_account_iam_member" "workload_identity_user" {
  service_account_id = google_service_account.kubernetes_sa.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "serviceAccount:${var.project_id}.svc.id.goog[default/itinerary-service-sa]"
}

# Store database password in Secret Manager
resource "google_secret_manager_secret" "db_password" {
  secret_id = local.secret_name

  replication {
    auto {}
  }

  depends_on = [google_project_service.required_apis]

  lifecycle {
    prevent_destroy = false
    # If resource exists, import it instead of failing
    # Run: terraform import google_secret_manager_secret.db_password projects/PROJECT_ID/secrets/SECRET_NAME
  }
}

resource "google_secret_manager_secret_version" "db_password" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = var.db_password
}

# IAM Binding - Secret Manager Secret Accessor
resource "google_secret_manager_secret_iam_member" "kubernetes_secret_accessor" {
  secret_id = google_secret_manager_secret.db_password.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.kubernetes_sa.email}"
}

# Cloud SQL Instance
resource "google_sql_database_instance" "main" {
  name             = local.db_instance_name
  database_version = "POSTGRES_16"
  region           = var.region

  settings {
    tier              = var.db_tier
    availability_type = var.db_availability_type
    disk_size         = 10
    disk_type         = "PD_SSD"

    backup_configuration {
      enabled            = true
      start_time         = "03:00"
      point_in_time_recovery_enabled = false
    }

    ip_configuration {
      ipv4_enabled    = true
      private_network = null
    }

    database_flags {
      name  = "max_connections"
      value = "100"
    }

    insights_config {
      query_insights_enabled  = true
      query_plans_per_minute  = 5
      query_string_length     = 1024
      record_application_tags = true
    }
  }

  deletion_protection = false  # Set to true for production!

  depends_on = [google_project_service.required_apis]

  lifecycle {
    prevent_destroy = false
    # If resource exists, import it instead of failing
    # Run: terraform import google_sql_database_instance.main PROJECT_ID/INSTANCE_NAME
  }
}

# Cloud SQL Database
resource "google_sql_database" "database" {
  name     = var.db_name
  instance = google_sql_database_instance.main.name

  lifecycle {
    prevent_destroy = false
    # If resource exists, import it instead of failing
    # Run: terraform import google_sql_database.database projects/PROJECT_ID/instances/INSTANCE_NAME/databases/DB_NAME
  }
}

# Cloud SQL User
resource "google_sql_user" "user" {
  name     = var.db_user
  instance = google_sql_database_instance.main.name
  password = var.db_password

  depends_on = [google_secret_manager_secret_version.db_password]

  lifecycle {
    prevent_destroy = false
  }
}

# Artifact Registry Repository (optional - only if not using external registry)
resource "google_artifact_registry_repository" "docker_repo" {
  count = var.create_artifact_registry ? 1 : 0

  location      = var.region
  repository_id = var.artifact_registry_name
  description   = "Docker repository for ${var.app_name}"
  format        = "DOCKER"

  labels = var.labels

  depends_on = [google_project_service.required_apis]

  lifecycle {
    prevent_destroy = false
    # If resource exists, import it instead of failing
    # Run: terraform import 'google_artifact_registry_repository.docker_repo[0]' projects/PROJECT_ID/locations/REGION/repositories/REPO_NAME
  }
}

# Cloud Storage Bucket
resource "google_storage_bucket" "app_bucket" {
  name          = local.bucket_name
  location      = var.bucket_location
  force_destroy = false

  uniform_bucket_level_access = true

  cors {
    origin          = ["*"]
    method          = ["GET", "HEAD", "PUT", "POST", "DELETE"]
    response_header = ["*"]
    max_age_seconds = 3600
  }

  labels = var.labels

  depends_on = [google_project_service.required_apis]

  lifecycle {
    prevent_destroy = false
    # If resource exists, import it instead of failing
    # Run: terraform import google_storage_bucket.app_bucket BUCKET_NAME
  }
}

# IAM Binding - Storage Object Admin (for reading/writing objects)
resource "google_storage_bucket_iam_member" "kubernetes_storage_admin" {
  bucket = google_storage_bucket.app_bucket.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.kubernetes_sa.email}"
}

# IAM Binding - Storage Admin (for creating signed URLs)
# This role includes permissions needed for URL signing
resource "google_project_iam_member" "kubernetes_storage_admin_project" {
  project = var.project_id
  role    = "roles/storage.admin"
  member  = "serviceAccount:${google_service_account.kubernetes_sa.email}"
}

# Firestore Database
resource "google_firestore_database" "database" {
  project     = var.project_id
  name        = "(default)"
  location_id = var.firestore_location
  type        = "FIRESTORE_NATIVE"

  depends_on = [google_project_service.required_apis]

  lifecycle {
    prevent_destroy = false  # Set to true for production
    # If resource exists, import it instead of failing
    # Run: terraform import google_firestore_database.database "(default)"
  }
}

# Indexes are already created! Comment in when needed
# Comments Indexes
/*resource "google_firestore_index" "comments_by_itinerary" {
  project    = var.project_id
  collection = "comments"

  fields {
    field_path = "itineraryId"
    order      = "ASCENDING"
  }

  fields {
    field_path = "createdAt"
    order      = "DESCENDING"
  }
}

resource "google_firestore_index" "comments_by_user" {
  project    = var.project_id
  collection = "comments"

  fields {
    field_path = "userEmail"
    order      = "ASCENDING"
  }

  fields {
    field_path = "createdAt"
    order      = "DESCENDING"
  }
}

# Likes Indexes
resource "google_firestore_index" "likes_by_user" {
  project    = var.project_id
  collection = "likes"

  fields {
    field_path = "userEmail"
    order      = "ASCENDING"
  }

  fields {
    field_path = "createdAt"
    order      = "DESCENDING"
  }
}*/

# Data source for project information
data "google_project" "project" {
  project_id = var.project_id
}

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
  
  authorized_domains = [
    "graphite-plane-474510-s9.firebaseapp.com",
    "graphite-plane-474510-s9.web.app",
    "tripico.fun",
    "frontend.tripico.fun",
    "api.tripico.fun",
  ]

  depends_on = [google_project_service.identitytoolkit]
}

# ============================================================================
# Kubernetes (GKE) Infrastructure
# ============================================================================

# VPC Network for GKE
resource "google_compute_network" "gke_network" {
  name                    = "${var.app_name}-network"
  auto_create_subnetworks = false
  enable_ula_internal_ipv6 = true

  depends_on = [google_project_service.required_apis]

  lifecycle {
    prevent_destroy = false
  }
}

# Subnetwork for GKE with secondary IP ranges for services and pods
resource "google_compute_subnetwork" "gke_subnet" {
  name          = "${var.app_name}-subnet"
  ip_cidr_range = var.gke_subnet_cidr
  region        = var.region
  network       = google_compute_network.gke_network.id

  stack_type       = "IPV4_IPV6"
  ipv6_access_type = "INTERNAL"

  secondary_ip_range {
    range_name    = "services-range"
    ip_cidr_range = var.gke_services_cidr
  }

  secondary_ip_range {
    range_name    = "pod-ranges"
    ip_cidr_range = var.gke_pods_cidr
  }

  depends_on = [google_compute_network.gke_network]

  lifecycle {
    prevent_destroy = false
  }
}

# GKE Autopilot Cluster
resource "google_container_cluster" "main" {
  name     = "${var.app_name}-cluster"
  location = var.region

  enable_autopilot         = true
  enable_l4_ilb_subsetting = true

  network    = google_compute_network.gke_network.id
  subnetwork = google_compute_subnetwork.gke_subnet.id

  ip_allocation_policy {
    stack_type                    = "IPV4_IPV6"
    services_secondary_range_name = google_compute_subnetwork.gke_subnet.secondary_ip_range[0].range_name
    cluster_secondary_range_name  = google_compute_subnetwork.gke_subnet.secondary_ip_range[1].range_name
  }

  deletion_protection = false

  depends_on = [
    google_project_service.required_apis,
    google_compute_subnetwork.gke_subnet,
  ]

  lifecycle {
    prevent_destroy = false
  }
}

# ============================================================================
# Google Cloud API Gateway
# ============================================================================

# API Gateway API
resource "google_api_gateway_api" "api_gateway" {
  provider     = google-beta
  api_id       = "${var.app_name}-api"
  display_name = "${var.app_name} API Gateway"
  project      = var.project_id

  depends_on = [google_project_service.required_apis]
}

# API Gateway API Config
resource "google_api_gateway_api_config" "api_config" {
  provider = google-beta
  api      = google_api_gateway_api.api_gateway.api_id

  # Stable based on content, NOT timestamp
  api_config_id = "${var.app_name}-api-config-${substr(sha256(templatefile("${path.module}/api-gateway-config.yaml", {
    app_name           = var.app_name
    services           = var.microservices
    firebase_project_id = var.project_id
  })), 0, 8)}"

  display_name = "${var.app_name} API Config"

  openapi_documents {
    document {
      path     = "api-config.yaml"
      contents = base64encode(templatefile("${path.module}/api-gateway-config.yaml", {
        app_name           = var.app_name
        services           = var.microservices
        firebase_project_id = var.project_id
      }))
    }
  }

  # Only include this if backend requires IAM tokens
  # Otherwise: remove gateway_config entirely.
  gateway_config {
    backend_config {
      google_service_account = google_service_account.kubernetes_sa.email
    }
  }

  lifecycle {
    create_before_destroy = true
  }

  depends_on = [
    google_api_gateway_api.api_gateway,
    google_service_account.kubernetes_sa,
  ]
}

# API Gateway instance
resource "google_api_gateway_gateway" "api_gateway" {
  provider    = google-beta
  api_config  = google_api_gateway_api_config.api_config.id
  gateway_id  = "${var.app_name}-gateway"
  display_name = "${var.app_name} Gateway"
  region      = var.region
  project     = var.project_id

  depends_on = [
    google_api_gateway_api_config.api_config
  ]
}
