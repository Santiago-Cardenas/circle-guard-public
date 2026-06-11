# Nos conectamos al Kubernetes que trae Docker Desktop (contexto docker-desktop).
provider "kubernetes" {
  config_path    = "~/.kube/config"
  config_context = "docker-desktop"
}
