variable "namespace_name" {
  description = "Namespace de este ambiente"
  type        = string
}

variable "cpu_quota" {
  description = "CPU maxima del namespace"
  type        = string
}

variable "memory_quota" {
  description = "Memoria maxima del namespace"
  type        = string
}

variable "pods_quota" {
  description = "Numero maximo de pods"
  type        = string
}
