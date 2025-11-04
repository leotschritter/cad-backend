# ===================================================
# Statische externe IP
# ===================================================

resource "google_compute_address" "app_vm_static_ip" {
  name   = "cad-travel-vm-static-ip"
  region = var.region

  labels = {
    environment = var.environment
    app         = "cad-travel"
    managed_by  = "terraform"
  }
}

# ===================================================
# Compute Engine VM für IaaS Deployment
# ===================================================

# VM Instanz
resource "google_compute_instance" "app_vm" {
  name         = "cad-travel-app-vm"
  machine_type = var.vm_machine_type
  zone         = var.zone

  boot_disk {
    initialize_params {
      image = "ubuntu-os-cloud/ubuntu-2204-lts"
      size  = 30
      type  = "pd-standard"
    }
  }

  network_interface {
    network = "default"
    access_config {
        nat_ip = google_compute_address.app_vm_static_ip.address
    }
  }

  # Service Account mit Zugriff auf Cloud SQL + Firestore
  service_account {
    email  = google_service_account.app_sa.email
    scopes = ["cloud-platform"]
  }

  # Startup Script mit Template-Variablen
  metadata_startup_script = templatefile("${path.module}/startup-script.sh", {
    project_id          = var.project_id
    db_instance_ip      = google_sql_database_instance.main.public_ip_address
    db_name             = var.db_name
    db_user             = var.db_user
    db_password         = random_password.db_password.result
    service_account_key = google_service_account_key.app_sa_key.private_key
    service_account_email = google_service_account.app_sa.email
    backend_image       = var.backend_image
    frontend_image      = var.frontend_image
    storage_bucket      = google_storage_bucket.images.name

  })

  # Metadaten
  metadata = {
    enable-oslogin = "TRUE"
  }

  tags = ["http-server", "https-server"]

  labels = {
    environment = var.environment
    app         = "cad-travel"
    managed_by  = "terraform"
  }

  # VM sollte bei Änderungen neu erstellt werden (außer Metadaten)
  lifecycle {
    create_before_destroy = true
  }

  depends_on = [
    google_sql_database_instance.main,
    google_service_account.app_sa,
    google_storage_bucket.images,
    google_project_service.compute
  ]
}

# Firewall-Regel für Frontend (5173) und Backend (8080)
resource "google_compute_firewall" "allow_app" {
  name    = "allow-cad-travel-app"
  network = "default"

  allow {
    protocol = "tcp"
    ports    = ["5173", "8080"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["http-server"]

  description = "Allow traffic to CAD Travel App (Frontend: 5173, Backend: 8080)"
}

# Firewall-Regel für HTTPS (443) hinzufügen
resource "google_compute_firewall" "allow_https" {
  name    = "allow-https-cad-travel"
  network = "default"

  allow {
    protocol = "tcp"
    ports    = ["80", "443"]  # ← HTTP + HTTPS
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["http-server"]

  description = "Allow HTTPS traffic to CAD Travel App"
}


# Firewall-Regel für SSH
resource "google_compute_firewall" "allow_ssh" {
  name    = "allow-ssh-cad-travel"
  network = "default"

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = var.allowed_ssh_ips
  target_tags   = ["http-server"]

  description = "Allow SSH access to CAD Travel App VM"
}

# ===================================================
# Outputs
# ===================================================

output "vm_external_ip" {
  description = "External IP der VM"
  value       = google_compute_instance.app_vm.network_interface[0].access_config[0].nat_ip
}

output "vm_internal_ip" {
  description = "Internal IP der VM"
  value       = google_compute_instance.app_vm.network_interface[0].network_ip
}

output "frontend_url" {
  description = "Frontend URL"
  value       = "http://${google_compute_instance.app_vm.network_interface[0].access_config[0].nat_ip}:5173"
}

output "backend_url" {
  description = "Backend URL"
  value       = "http://${google_compute_instance.app_vm.network_interface[0].access_config[0].nat_ip}:8080"
}

output "backend_api_docs" {
  description = "Backend API Dokumentation"
  value       = "http://${google_compute_instance.app_vm.network_interface[0].access_config[0].nat_ip}:8080/q/swagger-ui"
}

output "ssh_command" {
  description = "SSH Befehl zur VM"
  value       = "gcloud compute ssh ${google_compute_instance.app_vm.name} --zone=${var.zone} --project=${var.project_id}"
}

output "logs_command" {
  description = "Befehl zum Anzeigen der Docker Logs"
  value       = "gcloud compute ssh ${google_compute_instance.app_vm.name} --zone=${var.zone} --project=${var.project_id} --command='cd /opt/cad-travel && sudo docker-compose logs -f'"
}
