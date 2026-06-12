#!/usr/bin/env bash
# =====================================================================
# FASE 6 Bono Chaos Engineering — Instala Chaos Mesh en el cluster local.
#
# Uso: bash infra/k8s/chaos/00-install-chaos-mesh.sh
# Requiere: helm, kubectl
# =====================================================================
set -euo pipefail

CHAOS_VERSION="2.6.3"

echo ">> Agregando repositorio de Chaos Mesh a Helm..."
helm repo add chaos-mesh https://charts.chaos-mesh.org
helm repo update

echo ">> Creando namespace chaos-testing..."
kubectl create namespace chaos-testing --dry-run=client -o yaml | kubectl apply -f -

echo ">> Instalando Chaos Mesh v${CHAOS_VERSION} (runtime: containerd para Docker Desktop)..."
helm upgrade --install chaos-mesh chaos-mesh/chaos-mesh \
  --namespace chaos-testing \
  --version "${CHAOS_VERSION}" \
  --set chaosDaemon.runtime=containerd \
  --set chaosDaemon.socketPath=/run/containerd/containerd.sock \
  --set dashboard.securityMode=false \
  --set controllerManager.replicaCount=1 \
  --wait --timeout=5m

echo ""
echo ">> Chaos Mesh instalado correctamente."
echo ">> Dashboard: kubectl port-forward -n chaos-testing svc/chaos-dashboard 2333:2333"
echo ">> Luego abre: http://localhost:2333"
echo ""
echo ">> Para aplicar los experimentos:"
echo "   kubectl apply -f infra/k8s/chaos/01-pod-kill-gateway.yaml"
echo "   kubectl apply -f infra/k8s/chaos/02-network-delay-identity.yaml"
echo "   kubectl apply -f infra/k8s/chaos/03-cpu-stress-promotion.yaml"
