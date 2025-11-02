# ===================================================
# Firestore Database (NoSQL)
# ===================================================

# Firestore Database
# HINWEIS: Falls die Datenbank bereits existiert, importiere sie mit:
# terraform import google_firestore_database.database "projects/graphite-plane-474510-s9/databases/(default)"
resource "google_firestore_database" "database" {
  project     = var.project_id
  name        = "(default)"
  location_id = var.firestore_location
  type        = "FIRESTORE_NATIVE"

  # Concurrency Mode
  concurrency_mode = "OPTIMISTIC"

  # Point-in-time Recovery (für Produktion empfohlen)
  point_in_time_recovery_enablement = "POINT_IN_TIME_RECOVERY_DISABLED"

  # Delete Protection (für Produktion auf ENABLED setzen!)
  delete_protection_state = "DELETE_PROTECTION_DISABLED"

  # Lifecycle: Verhindere versehentliches Löschen
  lifecycle {
    prevent_destroy = false # Für erste Tests auf false, später auf true
  }

  depends_on = [google_project_service.firestore]
}

# Output
output "firestore_database_name" {
  description = "Firestore Database Name"
  value       = google_firestore_database.database.name
}

output "firestore_location" {
  description = "Firestore Location"
  value       = google_firestore_database.database.location_id
}

