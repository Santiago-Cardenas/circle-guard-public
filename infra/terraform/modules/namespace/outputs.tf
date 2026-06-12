# Devolvemos el nombre del namespace por si otro modulo lo necesita.
output "namespace" {
  description = "Nombre del namespace creado"
  value       = kubernetes_namespace.this.metadata[0].name
}
