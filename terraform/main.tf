# ===================================================
# Terraform Hauptkonfiguration für IaaS Deployment
# ===================================================

# Aktiviere benötigte APIs

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

resource "google_project_service" "compute" {
  service            = "compute.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "sql_admin" {
  service            = "sqladmin.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "firestore" {
  service            = "firestore.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "storage" {
  service            = "storage.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "iam" {
  service            = "iam.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "service_networking" {
  service            = "servicenetworking.googleapis.com"
  disable_on_destroy = false
}

# Firebase/Identity Platform APIs
resource "google_project_service" "firebase" {
  service            = "firebase.googleapis.com"
  disable_on_destroy = false
}




