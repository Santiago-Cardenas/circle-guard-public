# Este modulo crea un namespace y le pone limites de recursos.
# Lo reutilizamos en dev, stage y prod cambiando solo los valores.

# 1) El namespace donde van a vivir los servicios del ambiente
resource "kubernetes_namespace" "this" {
  metadata {
    name = var.namespace_name
    labels = {
      env     = var.environment
      project = var.project
    }
  }
}

# 2) Cuota total de recursos para todo el namespace
#    Asi un ambiente no se come toda la maquina.
resource "kubernetes_resource_quota" "this" {
  metadata {
    name      = "cuota-${var.environment}"
    namespace = kubernetes_namespace.this.metadata[0].name
  }

  spec {
    hard = {
      "requests.cpu"    = var.cpu_quota
      "requests.memory" = var.memory_quota
      "limits.cpu"      = var.cpu_quota
      "limits.memory"   = var.memory_quota
      "pods"            = var.pods_quota
    }
  }
}

# 3) Valores por defecto para los contenedores que no declaren limites
resource "kubernetes_limit_range" "this" {
  metadata {
    name      = "limites-${var.environment}"
    namespace = kubernetes_namespace.this.metadata[0].name
  }

  spec {
    limit {
      type = "Container"

      default = {
        cpu    = var.default_cpu
        memory = var.default_memory
      }

      default_request = {
        cpu    = var.request_cpu
        memory = var.request_memory
      }
    }
  }
}
