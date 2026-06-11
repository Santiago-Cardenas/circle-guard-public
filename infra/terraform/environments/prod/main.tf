# Ambiente PROD: usamos el modulo del namespace con los valores de produccion.
module "namespace" {
  source = "../../modules/namespace"

  environment    = "prod"
  namespace_name = var.namespace_name
  cpu_quota      = var.cpu_quota
  memory_quota   = var.memory_quota
  pods_quota     = var.pods_quota
}
