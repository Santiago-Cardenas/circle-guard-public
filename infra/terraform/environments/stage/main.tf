# Ambiente STAGE: usamos el modulo del namespace con los valores de pruebas.
module "namespace" {
  source = "../../modules/namespace"

  environment    = "stage"
  namespace_name = var.namespace_name
  cpu_quota      = var.cpu_quota
  memory_quota   = var.memory_quota
  pods_quota     = var.pods_quota
}
