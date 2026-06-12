variable "cluster_name" {
  description = "Nombre del cluster EKS"
  type        = string
  default     = "circleguard-eks"
}

variable "region" {
  description = "Region de AWS"
  type        = string
  default     = "us-east-1"
}

variable "kubernetes_version" {
  description = "Version de Kubernetes"
  type        = string
  default     = "1.30"
}

variable "node_instance_type" {
  description = "Tipo de instancia EC2 para los nodos"
  type        = string
  default     = "t3.small"
}

variable "node_desired_size" {
  description = "Numero de nodos deseados"
  type        = number
  default     = 2
}

variable "node_min_size" {
  type    = number
  default = 1
}

variable "node_max_size" {
  type    = number
  default = 3
}

variable "environment" {
  type    = string
  default = "cloud"
}
