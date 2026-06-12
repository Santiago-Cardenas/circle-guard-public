variable "location" {
  type    = string
  default = "eastus"
}

variable "cluster_name" {
  type    = string
  default = "circleguard-aks"
}

variable "resource_group_name" {
  type    = string
  default = "circleguard-rg"
}

variable "node_vm_size" {
  type    = string
  default = "Standard_B2s"
}

variable "node_count" {
  type    = number
  default = 2
}
