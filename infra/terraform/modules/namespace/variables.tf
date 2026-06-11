# Variables del modulo de namespace.
# Cada ambiente (dev, stage, prod) le pasa sus propios valores.

variable "environment" {
  description = "Nombre del ambiente: dev, stage o prod"
  type        = string
}

variable "namespace_name" {
  description = "Nombre del namespace en Kubernetes"
  type        = string
}

variable "project" {
  description = "Nombre del proyecto para las etiquetas"
  type        = string
  default     = "circleguard"
}

variable "cpu_quota" {
  description = "CPU maxima para todo el namespace"
  type        = string
}

variable "memory_quota" {
  description = "Memoria maxima para todo el namespace"
  type        = string
}

variable "pods_quota" {
  description = "Numero maximo de pods permitidos en el namespace"
  type        = string
}

variable "default_cpu" {
  description = "CPU por defecto que se le asigna a un contenedor"
  type        = string
  default     = "250m"
}

variable "default_memory" {
  description = "Memoria por defecto que se le asigna a un contenedor"
  type        = string
  default     = "512Mi"
}

variable "request_cpu" {
  description = "CPU minima que pide un contenedor"
  type        = string
  default     = "100m"
}

variable "request_memory" {
  description = "Memoria minima que pide un contenedor"
  type        = string
  default     = "256Mi"
}
