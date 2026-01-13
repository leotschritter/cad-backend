# =============================================================================
# Enterprise Tier Tenant Outputs
# =============================================================================
# Outputs for tenant-specific resources only.
# =============================================================================

# Tenant Information
output "tenant_name" {
  description = "The name of this enterprise tenant"
  value       = var.tenant_name
}

output "project_id" {
  description = "The GCP project ID"
  value       = var.project_id
}

output "region" {
  description = "The GCP region"
  value       = var.region
}

output "domain_name" {
  description = "The domain name for this enterprise tenant"
  value       = var.domain_name
}

# Storage Outputs
output "bucket_name" {
  description = "The name of the tenant's Cloud Storage bucket"
  value       = module.storage.bucket_name
}

output "bucket_url" {
  description = "The URL of the tenant's Cloud Storage bucket"
  value       = module.storage.bucket_url
}

# Service Account Outputs (using shared service account from freemium)
output "service_account_email" {
  description = "The email of the shared service account used by this tenant"
  value       = local.shared_service_account_email
}

output "service_account_name" {
  description = "The name of the shared service account used by this tenant"
  value       = local.shared_service_account_name
}

output "workload_identity_bindings" {
  description = "Workload Identity bindings created for this tenant's namespace"
  value       = [for k, v in google_service_account_iam_member.workload_identity_bindings : v.member]
}

# API Gateway Outputs
output "api_gateway_name" {
  description = "The name of the tenant's API Gateway"
  value       = module.api_gateway.api_gateway_name
}

output "api_gateway_gateway_name" {
  description = "The name of the API Gateway gateway"
  value       = module.api_gateway.api_gateway_gateway_name
}

output "api_gateway_url" {
  description = "The URL of the tenant's API Gateway"
  value       = module.api_gateway.api_gateway_url
}

output "api_gateway_default_hostname" {
  description = "The default hostname for the API Gateway"
  value       = module.api_gateway.api_gateway_default_hostname
}

# Service URLs (all services are dedicated for enterprise tier)
output "service_urls" {
  description = "Important service URLs for this enterprise tenant"
  value = {
    # API Gateway URL (for public API access)
    api_gateway = module.api_gateway.api_gateway_url

    # Tenant-specific API endpoints via API Gateway
    comment_api   = "${module.api_gateway.api_gateway_url}/comment"
    itinerary_api = "${module.api_gateway.api_gateway_url}/itinerary"
    like_api      = "${module.api_gateway.api_gateway_url}/like"
    location_api  = "${module.api_gateway.api_gateway_url}/location"
    user_api      = "${module.api_gateway.api_gateway_url}/user"
    feed_api      = "${module.api_gateway.api_gateway_url}/feed"
    graph_api     = "${module.api_gateway.api_gateway_url}/graph"

    # Dedicated services (enterprise gets their own, not shared)
    warnings_api = "${module.api_gateway.api_gateway_url}/warnings"
    weather_api  = "${module.api_gateway.api_gateway_url}/api/weather"
  }
}

# Microservices Configuration (for reference)
output "microservices" {
  description = "Microservices configuration for this enterprise tenant"
  value       = local.microservices
}
