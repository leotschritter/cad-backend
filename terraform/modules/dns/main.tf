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
# Create a Google Cloud DNS managed zone for full control over the DNS settings of the domain tripico.fun.
resource "google_dns_managed_zone" "tripico_fun" {
  project     = var.project_id
  name        = var.cloud_dns_managed_zone_name
  dns_name    = "${var.domain_name}."
  description = "Authoritative zone for tripico.fun"
}

resource "google_compute_address" "ingress_ip" {
  project = var.project_id
  name    = "tripico-ingress-ip"
  region  = var.region
}

resource "google_dns_record_set" "wildcard" {
  project      = var.project_id
  managed_zone = google_dns_managed_zone.tripico_fun.name
  name         = "*${var.domain_name_prefix}.${var.domain_name}."
  type         = "A"
  ttl          = 60

  rrdatas = [google_compute_address.ingress_ip.address]
}

resource "helm_release" "ingress_nginx" {
  name             = var.ingress_namespace
  repository       = "https://kubernetes.github.io/ingress-nginx"
  chart            = var.ingress_namespace
  namespace        = var.ingress_namespace
  create_namespace = true

  values = [yamlencode({
    controller = {
      service = {
        type           = "LoadBalancer"
        loadBalancerIP = google_compute_address.ingress_ip.address
      }
    }
  })]
}