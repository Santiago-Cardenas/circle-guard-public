variable "cluster_name" {
  description = "Nombre del cluster AKS"
  type        = string
  default     = "circleguard-aks"
}

variable "resource_group_name" {
  description = "Nombre del Resource Group en Azure"
  type        = string
  default     = "circleguard-rg"
}

variable "location" {
  description = "Region de Azure"
  type        = string
  default     = "eastus"
}

variable "kubernetes_version" {
  description = "Version de Kubernetes"
  type        = string
  default     = "1.30"
}

variable "node_vm_size" {
  description = "Tamano de VM para los nodos"
  type        = string
  default     = "Standard_B2s"
}

variable "node_count" {
  description = "Numero de nodos"
  type        = number
  default     = 2
}

variable "environment" {
  type    = string
  default = "cloud"
}
