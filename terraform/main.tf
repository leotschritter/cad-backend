# Enable required Google Cloud APIs
resource "google_project_service" "required_apis" {
  for_each = toset([
    "sqladmin.googleapis.com",
    "run.googleapis.com",
    "artifactregistry.googleapis.com",
    "firestore.googleapis.com",
    "storage-api.googleapis.com",
    "iamcredentials.googleapis.com",
    "secretmanager.googleapis.com",
    "compute.googleapis.com",
    "vpcaccess.googleapis.com",
  ])

  service            = each.value
  disable_on_destroy = false
}

# Service Account for Cloud Run
resource "google_service_account" "cloud_run_sa" {
  account_id   = "${var.app_name}-sa"
  display_name = "Cloud Run SA for ${var.app_name}"
  description  = "Service account for Cloud Run service to access Cloud SQL, Firestore, and Cloud Storage"

  depends_on = [google_project_service.required_apis]
}

# IAM Binding - Cloud SQL Client
resource "google_project_iam_member" "cloud_run_sql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.cloud_run_sa.email}"
}

# IAM Binding - Firestore User
resource "google_project_iam_member" "cloud_run_firestore_user" {
  project = var.project_id
  role    = "roles/datastore.user"
  member  = "serviceAccount:${google_service_account.cloud_run_sa.email}"
}

# IAM Binding - Service Account Token Creator
resource "google_service_account_iam_member" "cloud_run_token_creator" {
  service_account_id = google_service_account.cloud_run_sa.name
  role               = "roles/iam.serviceAccountTokenCreator"
  member             = "serviceAccount:${google_service_account.cloud_run_sa.email}"
}

# Generate random password for database
resource "random_password" "db_password" {
  length  = 32
  special = true
}

# Store database password in Secret Manager
resource "google_secret_manager_secret" "db_password" {
  secret_id = "${var.app_name}-db-password"

  replication {
    auto {}
  }

  depends_on = [google_project_service.required_apis]
}

resource "google_secret_manager_secret_version" "db_password" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = random_password.db_password.result
}

# IAM Binding - Secret Manager Secret Accessor
resource "google_secret_manager_secret_iam_member" "cloud_run_secret_accessor" {
  secret_id = google_secret_manager_secret.db_password.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloud_run_sa.email}"
}

# Cloud SQL Instance
resource "google_sql_database_instance" "main" {
  name             = var.db_instance_name
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
      ipv4_enabled    = false
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

  deletion_protection = true

  depends_on = [google_project_service.required_apis]
}

# Cloud SQL Database
resource "google_sql_database" "database" {
  name     = var.db_name
  instance = google_sql_database_instance.main.name
}

# Cloud SQL User
resource "google_sql_user" "user" {
  name     = var.db_user
  instance = google_sql_database_instance.main.name
  password = random_password.db_password.result
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
}

# Cloud Storage Bucket
resource "google_storage_bucket" "app_bucket" {
  name          = "${var.project_id}-${var.bucket_name}"
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
}

# IAM Binding - Storage Object Admin
resource "google_storage_bucket_iam_member" "cloud_run_storage_admin" {
  bucket = google_storage_bucket.app_bucket.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.cloud_run_sa.email}"
}

# Firestore Database
resource "google_firestore_database" "database" {
  project     = var.project_id
  name        = "(default)"
  location_id = var.firestore_location
  type        = "FIRESTORE_NATIVE"

  depends_on = [google_project_service.required_apis]
}

# Cloud Run Service
resource "google_cloud_run_v2_service" "main" {
  name     = var.cloud_run_service_name
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.cloud_run_sa.email

    scaling {
      min_instance_count = var.cloud_run_min_instances
      max_instance_count = var.cloud_run_max_instances
    }

    timeout = "${var.cloud_run_timeout}s"
      image = var.docker_image_url != "" ? var.docker_image_url : "${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_name}/${var.app_name}:${var.app_version}"
    containers {
      image = "${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_registry_name}/${var.app_name}:${var.app_version}"

      resources {
        limits = {
          cpu    = var.cloud_run_cpu
          memory = var.cloud_run_memory
        }
        cpu_idle = true
      }

      ports {
        container_port = 8080
      }

      env {
        name  = "DB_USER"
        value = var.db_user
      }

      env {
        name = "DB_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_password.secret_id
            version = "latest"
          }
        }
      }

      env {
        name  = "DB_URL"
        value = "jdbc:postgresql:///${var.db_name}?cloudSqlInstance=${google_sql_database_instance.main.connection_name}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
      }

      env {
        name  = "BUCKET_NAME"
        value = google_storage_bucket.app_bucket.name
      }

      env {
        name  = "PROJECT_ID"
        value = var.project_id
      }

      env {
        name  = "BACKEND_URL"
        value = "https://${var.cloud_run_service_name}-${data.google_project.project.number}.${var.region}.run.app"
      }

      env {
        name  = "SERVICE_ACCOUNT_EMAIL"
        value = google_service_account.cloud_run_sa.email
      }

      env {
        name  = "QUARKUS_PROFILE"
        value = "prod"
      }
    }

    # Add Cloud SQL connection
    vpc_access {
      egress = "PRIVATE_RANGES_ONLY"
    }
  }

  depends_on = concat(
    [
      google_project_service.required_apis,
      google_sql_database.database,
      google_sql_user.user,
      google_firestore_database.database,
      google_storage_bucket.app_bucket,
    ],
    var.create_artifact_registry ? [google_artifact_registry_repository.docker_repo[0]] : []
  )
    google_artifact_registry_repository.docker_repo,
  ]

  lifecycle {
    ignore_changes = [
      template[0].containers[0].image,
    ]
  }
}

# Data source for project information
data "google_project" "project" {
  project_id = var.project_id
}

# Cloud Run IAM - Allow public access (optional)
/* resource "google_cloud_run_v2_service_iam_member" "public_access" {
  count = var.allow_unauthenticated ? 1 : 0

  project  = google_cloud_run_v2_service.main.project
  location = google_cloud_run_v2_service.main.location
  name     = google_cloud_run_v2_service.main.name
  role     = "roles/run.invoker"
  member   = "allUsers"
} */

# Optional: Domain Mapping (requires domain verification)
# resource "google_cloud_run_domain_mapping" "default" {
#   count = var.domain_name != "" ? 1 : 0
#
#   location = var.region
#   name     = var.domain_name
#
#   metadata {
#     namespace = var.project_id
#   }
#
#   spec {
#     route_name = google_cloud_run_v2_service.main.name
#   }
# }

