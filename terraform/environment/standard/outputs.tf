# =============================================================================
# Standard Tier Tenant Outputs
# =============================================================================
# Outputs for tenant-specific resources only.
# =============================================================================

# Tenant Information
output "tenant_name" {
  description = "The name of this tenant"
  value       = var.tenant_name
}

# Identity Platform Tenant ID
# This is used by frontend to authenticate users with the correct tenant
# and by backend to validate that tokens belong to this tenant
output "identity_platform_tenant_id" {
  description = "The Identity Platform tenant ID for user isolation"
  value       = google_identity_platform_tenant.tenant.name
}

output "project_id" {
  description = "The GCP project ID"
  value       = var.project_id
}

output "region" {
  description = "The GCP region"
  value       = var.region
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

# Service URLs
output "service_urls" {
  description = "Important service URLs for this tenant"
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

    # Shared services (routed through tenant's gateway)
    warnings_api = "${module.api_gateway.api_gateway_url}/warnings"
    weather_api  = "${module.api_gateway.api_gateway_url}/api/weather"
  }
}

# Microservices Configuration (for reference)
output "microservices" {
  description = "Microservices configuration for this tenant"
  value       = local.microservices
}
