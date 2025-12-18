terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "6.38.0"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.13"
    }
  }
}
# Parent zone (prod only)
resource "google_dns_managed_zone" "tripico_fun" {
  count = var.is_prod_environment ? 1 : 0

  project     = var.project_id
  name        = var.cloud_dns_managed_zone_name
  dns_name    = "${var.domain_name}."
  description = "Authoritative zone for ${var.domain_name}"

  depends_on = [var.project_apis_enabled]
}

# Delegated subdomain zone (dev only)
resource "google_dns_managed_zone" "dev_tripico_fun" {
  count = var.is_prod_environment ? 0 : 1

  project     = var.project_id
  name        = var.cloud_dns_managed_zone_name
  dns_name    = "${var.domain_name}."
  description = "Delegated zone for ${var.domain_name}"

  depends_on = [var.project_apis_enabled]
}

# Static IP for ingress (both environments)
resource "google_compute_address" "ingress_ip" {
  project = var.project_id
  name    = "tripico-ingress-ip"
  region  = var.region
}

# DNS record in parent zone (prod only)
resource "google_dns_record_set" "wildcard" {
  count = var.is_prod_environment ? 1 : 0

  project      = var.project_id
  managed_zone = google_dns_managed_zone.tripico_fun[0].name
  name         = "*.${var.domain_name}."
  type         = "A"
  ttl          = 60

  rrdatas = [google_compute_address.ingress_ip.address]
}

# DNS record in delegated zone (dev only)
resource "google_dns_record_set" "dev_wildcard" {
  count = var.is_prod_environment ? 0 : 1

  project      = var.project_id
  managed_zone = google_dns_managed_zone.dev_tripico_fun[0].name
  name         = "*.${var.domain_name}."
  type         = "A"
  ttl          = 60

  rrdatas = [google_compute_address.ingress_ip.address]
}

/*# Delegation record in parent zone (prod only)
resource "google_dns_record_set" "dev_delegation" {
  count = var.is_prod_environment ? 1 : 0
  project      = var.project_id
  managed_zone = google_dns_managed_zone.tripico_fun[0].name
  name         = "dev.tripico.fun."
  type         = "NS"
  ttl          = 300
  rrdatas      = [
    # Paste nameservers from step 1
  ]
}*/

# Ingress controller (both environments)
resource "helm_release" "ingress_nginx" {
  name             = var.ingress_namespace
  repository       = "https://kubernetes.github.io/ingress-nginx"
  chart            = var.ingress_namespace
  namespace        = var.ingress_namespace
  create_namespace = true
  replace          = true
  force_update     = true

  values = [yamlencode({
    controller = {
      service = {
        type           = "LoadBalancer"
        loadBalancerIP = google_compute_address.ingress_ip.address
      }
    }
  })]
}