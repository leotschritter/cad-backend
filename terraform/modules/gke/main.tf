# VPC Network for GKE
resource "google_compute_network" "gke_network" {
  name                    = "${var.app_name}-network"
  auto_create_subnetworks = false
  enable_ula_internal_ipv6 = true

  depends_on = [var.project_apis_enabled]
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

  deletion_protection = var.deletion_protection

  depends_on = [
    var.project_apis_enabled,
    google_compute_subnetwork.gke_subnet,
  ]
}

