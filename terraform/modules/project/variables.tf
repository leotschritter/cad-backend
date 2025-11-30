variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "required_apis" {
  description = "List of Google Cloud APIs to enable"
  type        = list(string)
  default = [
    "sqladmin.googleapis.com",
    "artifactregistry.googleapis.com",
    "firestore.googleapis.com",
    "storage-api.googleapis.com",
    "iamcredentials.googleapis.com",
    "secretmanager.googleapis.com",
    "compute.googleapis.com",
    "container.googleapis.com",
    "apigateway.googleapis.com",
    "servicemanagement.googleapis.com",
    "servicecontrol.googleapis.com",
  ]
}

variable "authorized_domains" {
  description = "List of authorized domains for Identity Platform"
  type        = list(string)
  default = [
    "graphite-plane-474510-s9.firebaseapp.com",
    "graphite-plane-474510-s9.web.app",
    "tripico.fun",
    "frontend.tripico.fun",
    "api.tripico.fun",
    "iaas-476910.firebaseapp.com",
    "iaas-476910.web.app",
  ]
}

