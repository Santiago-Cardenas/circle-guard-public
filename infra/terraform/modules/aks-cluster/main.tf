# =====================================================================
# FASE 6 Bono D — Azure AKS Cluster
#
# Crea un cluster AKS con identidad gestionada y nodos Standard_B2s
# (2 vCPU / 4 GB RAM, ~$30/mes por nodo).
# Aplicar con: terraform -chdir=infra/terraform/environments/azure apply
# =====================================================================

resource "azurerm_resource_group" "this" {
  name     = var.resource_group_name
  location = var.location

  tags = { project = "circleguard", environment = var.environment }
}

resource "azurerm_kubernetes_cluster" "this" {
  name                = var.cluster_name
  location            = azurerm_resource_group.this.location
  resource_group_name = azurerm_resource_group.this.name
  dns_prefix          = var.cluster_name
  default_node_pool {
    name       = "default"
    node_count = var.node_count
    vm_size    = var.node_vm_size

    tags = { project = "circleguard" }
  }

  # Identidad gestionada — no requiere service principal manual
  identity {
    type = "SystemAssigned"
  }

  network_profile {
    network_plugin = "kubenet"
    load_balancer_sku = "standard"
  }

  tags = { project = "circleguard", environment = var.environment }
}
