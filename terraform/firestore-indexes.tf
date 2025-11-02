# Comments Indexes
resource "google_firestore_index" "comments_by_itinerary" {
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
}
