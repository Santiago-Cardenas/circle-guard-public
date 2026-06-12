# Version de Terraform y del provider de Kubernetes.
# El estado (tfstate) se guarda REMOTO dentro del cluster como un Secret.
terraform {
  required_version = ">= 1.5.0"

  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.30"
    }
  }

  backend "kubernetes" {
    secret_suffix = "circleguard-stage"
    namespace     = "default"
    config_path   = "~/.kube/config"
  }
}
