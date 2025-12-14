module "dns" {
  source = "./modules/dns"

  project_id   = var.project_id
  region       = var.region
}