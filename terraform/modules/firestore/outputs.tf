output "firestore_database_name" {
  description = "The name of the Firestore database"
  value       = google_firestore_database.database.name
}

output "firestore_location" {
  description = "The location of the Firestore database"
  value       = google_firestore_database.database.location_id
}

