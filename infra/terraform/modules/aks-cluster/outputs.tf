output "cluster_name" {
  value = azurerm_kubernetes_cluster.this.name
}

output "resource_group" {
  value = azurerm_resource_group.this.name
}

output "cluster_fqdn" {
  value = azurerm_kubernetes_cluster.this.fqdn
}

output "kubeconfig_command" {
  description = "Comando para actualizar el kubeconfig local"
  value       = "az aks get-credentials --resource-group ${azurerm_resource_group.this.name} --name ${azurerm_kubernetes_cluster.this.name}"
}

output "kube_config_raw" {
  value     = azurerm_kubernetes_cluster.this.kube_config_raw
  sensitive = true
}
