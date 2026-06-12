# Version de Terraform y del provider de Kubernetes.
# El estado (tfstate) se guarda REMOTO dentro del cluster como un Secret,
# asi no queda regado en el disco local y lo puede leer el pipeline.
terraform {
  required_version = ">= 1.5.0"

  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.30"
    }
  }

  backend "kubernetes" {
    secret_suffix = "circleguard-dev"
    namespace     = "default"
    config_path   = "~/.kube/config"
  }
}
