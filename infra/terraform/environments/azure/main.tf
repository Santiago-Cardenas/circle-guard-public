# Entorno cloud Azure — despliega AKS via modulo aks-cluster
module "aks" {
  source = "../../modules/aks-cluster"

  cluster_name        = var.cluster_name
  resource_group_name = var.resource_group_name
  location            = var.location
  node_vm_size        = var.node_vm_size
  node_count          = var.node_count
  environment         = "azure"
}
