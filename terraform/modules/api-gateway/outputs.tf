output "api_gateway_name" {
  description = "The name of the API Gateway"
  value       = google_api_gateway_api.api_gateway.api_id
}

output "api_gateway_gateway_name" {
  description = "The name of the API Gateway gateway"
  value       = google_api_gateway_gateway.api_gateway.gateway_id
}

output "api_gateway_url" {
  description = "The URL of the API Gateway"
  value       = "https://${google_api_gateway_gateway.api_gateway.default_hostname}"
}

output "api_gateway_default_hostname" {
  description = "The default hostname for the API Gateway"
  value       = google_api_gateway_gateway.api_gateway.default_hostname
}

