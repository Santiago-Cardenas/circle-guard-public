#!/usr/bin/env bash
# =====================================================================
# FASE 6 Bono FinOps — Instala metrics-server en el cluster local.
# Necesario para que HPA pueda leer metricas de CPU/memoria de los pods.
#
# Docker Desktop: el metrics-server no viene instalado por defecto.
# Uso: bash scripts/setup-metrics-server.sh
# =====================================================================
set -euo pipefail

echo ">> Instalando metrics-server en el cluster..."

# Descarga el manifest oficial y parchea --kubelet-insecure-tls
# (necesario en Docker Desktop porque el kubelet usa cert autofirmado)
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# Parchea el Deployment para agregar el flag de TLS inseguro (requerido en local)
kubectl patch deployment metrics-server \
  -n kube-system \
  --type=json \
  -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'

echo ">> Esperando a que metrics-server este listo (max 90s)..."
kubectl -n kube-system rollout status deployment/metrics-server --timeout=90s

echo ">> Verificando metricas de nodos:"
kubectl top nodes

echo ">> metrics-server instalado correctamente."
echo ">> HPA activo: kubectl get hpa -n circleguard-dev"
