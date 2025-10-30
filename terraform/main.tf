terraform {
  required_version = ">= 1.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# VPC Network
resource "google_compute_network" "vpc" {
  name                    = "cad-vpc"
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "subnet" {
  name          = "cad-subnet"
  ip_cidr_range = "10.0.0.0/24"
  region        = var.region
  network       = google_compute_network.vpc.id
}

# Firewall - HTTP/HTTPS
resource "google_compute_firewall" "allow_http" {
  name    = "cad-allow-http"
  network = google_compute_network.vpc.name

  allow {
    protocol = "tcp"
    ports    = ["80", "443", "8080", "5173"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["cad-server"]
}

# Firewall - SSH
resource "google_compute_firewall" "allow_ssh" {
  name    = "cad-allow-ssh"
  network = google_compute_network.vpc.name

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["cad-server"]
}

# Compute Instance
resource "google_compute_instance" "vm" {
  name         = "cad-travel-app"
  machine_type = var.machine_type
  zone         = var.zone
  tags         = ["cad-server"]

  boot_disk {
    initialize_params {
      image = "ubuntu-os-cloud/ubuntu-2204-lts"
      size  = 30
      type  = "pd-standard"
    }
  }

  network_interface {
    network    = google_compute_network.vpc.id
    subnetwork = google_compute_subnetwork.subnet.id

    access_config {
      # Ephemeral public IP
    }
  }

  metadata = {
    startup-script = templatefile("${path.module}/startup-script.sh", {
      db_user        = var.db_user
      db_password    = var.db_password
      db_name        = var.db_name
      backend_image  = "ghcr.io/leotschritter/cad-backend:latest"
      frontend_image = "ghcr.io/leotschritter/cad-frontend:latest"
    })
  }

  allow_stopping_for_update = true
}

# Reserve static IP (optional aber empfohlen)
resource "google_compute_address" "static_ip" {
  name   = "cad-static-ip"
  region = var.region
}
