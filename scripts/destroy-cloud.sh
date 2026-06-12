#!/usr/bin/env bash
# =====================================================================
# FASE 6 Bono D — Destruye los clusters cloud para evitar costos.
#
# Uso: bash scripts/destroy-cloud.sh [aws|azure|all]
# IMPORTANTE: Esta accion es IRREVERSIBLE. Confirmar antes de ejecutar.
# =====================================================================
set -euo pipefail

TARGET="${1:-all}"

destroy_aws() {
  echo ">> Destruyendo cluster EKS en AWS..."
  terraform -chdir=infra/terraform/environments/aws destroy -auto-approve
  echo ">> EKS destruido."
}

destroy_azure() {
  echo ">> Destruyendo cluster AKS en Azure..."
  terraform -chdir=infra/terraform/environments/azure destroy -auto-approve
  echo ">> AKS destruido."
}

echo "======================================================"
echo "  ADVERTENCIA: Se destruiran los recursos de: ${TARGET}"
echo "  Esta accion NO puede deshacerse."
echo "======================================================"
read -r -p "Escribi 'si' para confirmar: " confirm
if [ "${confirm}" != "si" ]; then
  echo "Cancelado."
  exit 0
fi

case "${TARGET}" in
  aws)   destroy_aws   ;;
  azure) destroy_azure ;;
  all)
    destroy_aws
    destroy_azure
    ;;
  *)
    echo "Uso: $0 [aws|azure|all]"
    exit 1
    ;;
esac

echo ""
echo ">> Recursos cloud eliminados. Volviendo al contexto local..."
kubectl config use-context docker-desktop
