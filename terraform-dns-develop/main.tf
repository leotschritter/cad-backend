module "dns" {
  source = "./modules/dns"

  project_id   = var.project_id
  region       = var.region
  domain_name  = var.domain_name
}