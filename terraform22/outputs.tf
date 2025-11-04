# ===================================================
# IaaS Deployment Zusammenfassung
# ===================================================

output "deployment_summary" {
  description = "Vollständige Deployment-Zusammenfassung"
  value       = <<-EOT

  ╔════════════════════════════════════════════════════════════════╗
  ║          CAD Travel App - IaaS Deployment erfolgreich!         ║
  ╚════════════════════════════════════════════════════════════════╝

  🌐 URLs:
  ─────────────────────────────────────────────────────────────────
  Frontend:     ${google_compute_instance.app_vm.network_interface[0].access_config[0].nat_ip}:5173
  Backend:      ${google_compute_instance.app_vm.network_interface[0].access_config[0].nat_ip}:8080
  API Docs:     ${google_compute_instance.app_vm.network_interface[0].access_config[0].nat_ip}:8080/q/swagger-ui

  💾 Datenbank:
  ─────────────────────────────────────────────────────────────────
  Cloud SQL IP: ${google_sql_database_instance.main.public_ip_address}
  DB Name:      ${var.db_name}
  DB User:      ${var.db_user}

  ☁️  Cloud Storage:
  ─────────────────────────────────────────────────────────────────
  Bucket:       ${google_storage_bucket.images.name}
  URL:          gs://${google_storage_bucket.images.name}

  🖥️  VM Informationen:
  ─────────────────────────────────────────────────────────────────
  VM Name:      ${google_compute_instance.app_vm.name}
  Zone:         ${var.zone}
  External IP:  ${google_compute_instance.app_vm.network_interface[0].access_config[0].nat_ip}
  Internal IP:  ${google_compute_instance.app_vm.network_interface[0].network_ip}

  📋 Nützliche Befehle:
  ─────────────────────────────────────────────────────────────────
  SSH zur VM:
    gcloud compute ssh ${google_compute_instance.app_vm.name} --zone=${var.zone} --project=${var.project_id}

  Container Logs anzeigen:
    gcloud compute ssh ${google_compute_instance.app_vm.name} --zone=${var.zone} --project=${var.project_id} \
      --command='cd /opt/cad-travel && sudo docker-compose logs -f'

  Container neustarten:
    gcloud compute ssh ${google_compute_instance.app_vm.name} --zone=${var.zone} --project=${var.project_id} \
      --command='cd /opt/cad-travel && sudo docker-compose restart'

  Images aktualisieren:
    gcloud compute ssh ${google_compute_instance.app_vm.name} --zone=${var.zone} --project=${var.project_id} \
      --command='cd /opt/cad-travel && sudo docker-compose pull && sudo docker-compose up -d'

  ╔════════════════════════════════════════════════════════════════╗
  ║  Warte 2-3 Minuten bis alle Container vollständig gestartet   ║
  ║  sind. Prüfe den Status mit den obigen Befehlen!              ║
  ╚════════════════════════════════════════════════════════════════╝

  EOT
}

