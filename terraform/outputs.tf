output "vm_external_ip" {
  value       = google_compute_instance.vm.network_interface[0].access_config[0].nat_ip
  description = "VM External IP"
}

output "backend_url" {
  value       = "http://${google_compute_instance.vm.network_interface[0].access_config[0].nat_ip}:8080"
  description = "Backend URL"
}

output "frontend_url" {
  value       = "http://${google_compute_instance.vm.network_interface[0].access_config[0].nat_ip}:5173"
  description = "Frontend URL"
}

output "ssh_command" {
  value       = "gcloud compute ssh cad-app-server --zone=${var.zone}"
  description = "SSH Command"
}
