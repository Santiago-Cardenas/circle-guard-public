# Entorno cloud AWS — despliega EKS via modulo eks-cluster
module "eks" {
  source = "../../modules/eks-cluster"

  cluster_name       = var.cluster_name
  region             = var.region
  node_instance_type = var.node_instance_type
  node_desired_size  = var.node_count
  node_min_size      = 1
  node_max_size      = var.node_count + 1
  environment        = "aws"
}
