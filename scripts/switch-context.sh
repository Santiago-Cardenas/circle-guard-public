#!/usr/bin/env bash
# =====================================================================
# FASE 6 Bono D — Cambia el contexto de kubectl entre entornos.
#
# Uso: bash scripts/switch-context.sh [local|aws|azure]
# =====================================================================
set -euo pipefail

CONTEXT="${1:-}"

case "${CONTEXT}" in
  local)
    kubectl config use-context docker-desktop
    echo ">> Contexto: docker-desktop (local)"
    ;;
  aws)
    CLUSTER=$(terraform -chdir=infra/terraform/environments/aws output -raw eks_cluster_name 2>/dev/null || echo "circleguard-eks")
    REGION=$(grep region infra/terraform/environments/aws/terraform.tfvars | awk -F'"' '{print $2}')
    aws eks update-kubeconfig --region "${REGION}" --name "${CLUSTER}"
    echo ">> Contexto: EKS ${CLUSTER} (${REGION})"
    ;;
  azure)
    CLUSTER=$(terraform -chdir=infra/terraform/environments/azure output -raw aks_cluster_name 2>/dev/null || echo "circleguard-aks")
    RG=$(terraform -chdir=infra/terraform/environments/azure output -raw resource_group 2>/dev/null || echo "circleguard-rg")
    az aks get-credentials --resource-group "${RG}" --name "${CLUSTER}" --overwrite-existing
    echo ">> Contexto: AKS ${CLUSTER} (${RG})"
    ;;
  *)
    echo "Uso: $0 [local|aws|azure]"
    echo ""
    echo "Contextos disponibles:"
    kubectl config get-contexts
    exit 1
    ;;
esac

echo ""
kubectl get nodes
