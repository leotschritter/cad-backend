# terraform/import.tf

import {
  to = google_compute_address.app_vm_static_ip
  id = "projects/iaas-476910/regions/europe-west3/addresses/cad-travel-vm-static-ip"
}

import {
  to = google_compute_firewall.allow_app
  id = "projects/iaas-476910/global/firewalls/allow-cad-travel-app"
}

import {
  to = google_compute_firewall.allow_https
  id = "projects/iaas-476910/global/firewalls/allow-https-cad-travel"
}

import {
  to = google_compute_firewall.allow_ssh
  id = "projects/iaas-476910/global/firewalls/allow-ssh-cad-travel"
}

import {
  to = google_sql_database_instance.main
  id = "projects/iaas-476910/instances/cad-travel-db"
}

/*import {
  to = google_sql_database.database
  id = "projects/iaas-476910/instances/cad-travel-db/databases/travel_app_db"
}*/
# Firestore composite indexes with actual IDs from gcloud
import {
  to = google_firestore_index.comments_by_itinerary
  id = "projects/iaas-476910/databases/(default)/collectionGroups/comments/indexes/CICAgOjXh4EK"
}

import {
  to = google_firestore_index.comments_by_user
  id = "projects/iaas-476910/databases/(default)/collectionGroups/comments/indexes/CICAgJjF9oIK"
}

import {
  to = google_firestore_index.likes_by_user
  id = "projects/iaas-476910/databases/(default)/collectionGroups/likes/indexes/CICAgJim14AK"
}
import {
  to = google_compute_instance.app_vm
  id = "projects/iaas-476910/zones/europe-west3-a/instances/cad-travel-app-vm"
}

import {
  to = google_sql_database.database
  id = "projects/iaas-476910/instances/cad-travel-db/databases/travel_app_db"
}

import {
  to = google_service_account.app_sa
  id = "projects/iaas-476910/serviceAccounts/cad-travel-app-sa@iaas-476910.iam.gserviceaccount.com"
}

import {
  to = google_identity_platform_config.default
  id = "projects/iaas-476910"
}

# GCS bucket: set to the real bucket name (not the resource name).
# Tip: gcloud storage buckets list --project=iaas-476910 --format="value(name)"
import {
  to = google_storage_bucket.images
  id = "iaas-476910-travel-images"
}