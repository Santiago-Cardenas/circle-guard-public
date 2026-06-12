#!/usr/bin/env bash
# =====================================================================
# FASE 6 Bono C — Service Mesh con Istio
#
# Instala Istio (perfil minimal) en el cluster local de Docker Desktop
# y habilita inyeccion de sidecar en circleguard-dev.
#
# Uso: bash infra/k8s/istio/00-install-istio.sh
# Requiere: kubectl, curl
# =====================================================================
set -euo pipefail

ISTIO_VERSION="1.22.1"

echo ">> Descargando istioctl ${ISTIO_VERSION}..."
curl -L https://istio.io/downloadIstio | ISTIO_VERSION=${ISTIO_VERSION} TARGET_ARCH=x86_64 sh -
export PATH="$PWD/istio-${ISTIO_VERSION}/bin:$PATH"

echo ">> Instalando Istio con perfil minimal..."
istioctl install --set profile=minimal -y

echo ">> Esperando que istiod este listo..."
kubectl -n istio-system rollout status deployment/istiod --timeout=3m

echo ">> Habilitando inyeccion de sidecar en circleguard-dev..."
kubectl label namespace circleguard-dev istio-injection=enabled --overwrite

echo ">> Aplicando configuracion de mTLS y politicas de trafico..."
kubectl apply -f infra/k8s/istio/01-mtls-peer-auth.yaml
kubectl apply -f infra/k8s/istio/02-virtual-services.yaml
kubectl apply -f infra/k8s/istio/03-destination-rules.yaml

echo ">> Reiniciando deployments para que reciban el sidecar Envoy..."
kubectl rollout restart deployment -n circleguard-dev

echo ""
echo ">> Istio instalado correctamente."
echo ">> Verifica estado: istioctl analyze -n circleguard-dev"
echo ">> Dashboard Kiali (si instalado): istioctl dashboard kiali"
echo ">> Dashboard Jaeger: kubectl port-forward -n istio-system svc/tracing 16686:80"
