output "aks_cluster_name" {
  value = module.aks.cluster_name
}

output "aks_cluster_fqdn" {
  value = module.aks.cluster_fqdn
}

output "kubeconfig_command" {
  value = module.aks.kubeconfig_command
}
