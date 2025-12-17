output "tripico_fun_name_servers" {
  description = "Authoritative name servers for tripico.fun (enter these at STRATO)"
  value = join("\n", google_dns_managed_zone.tripico_fun.name_servers)
}

output "domain_name" {
  description = "The domain name for the DNS zone"
  value       = var.domain_name
}

output "domain_name_prefix" {
  description = "The prefix to add to the domain name for the DNS records"
  value       = var.domain_name_prefix
}

output "cloud_dns_managed_zone_name" {
  description = "The name of the Cloud DNS managed zone"
  value       = var.cloud_dns_managed_zone_name
}

output "google_compute_address_ingress_ip" {
  description = "The IP address of the global load balancer"
  value       = google_compute_address.ingress_ip.address
}