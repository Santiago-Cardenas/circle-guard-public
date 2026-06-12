#!/usr/bin/env bash
# =====================================================================
# FASE 5 — Genera un certificado TLS autofirmado y crea el Secret
# `circleguard-tls` en el namespace indicado (por defecto circleguard-prod).
#
# Uso:
#   bash infra/k8s/security/gen-tls-secret.sh [namespace]
#
# Requiere: openssl y kubectl.
# =====================================================================
set -euo pipefail

NS="${1:-circleguard-prod}"
HOST="circleguard.local"
TMP="$(mktemp -d)"

echo ">> Generando certificado autofirmado para ${HOST} ..."
openssl req -x509 -nodes -newkey rsa:2048 \
  -keyout "${TMP}/tls.key" \
  -out    "${TMP}/tls.crt" \
  -days 365 \
  -subj "/CN=${HOST}/O=CircleGuard" \
  -addext "subjectAltName=DNS:${HOST}"

echo ">> Creando Secret circleguard-tls en el namespace ${NS} ..."
kubectl -n "${NS}" create secret tls circleguard-tls \
  --cert="${TMP}/tls.crt" \
  --key="${TMP}/tls.key" \
  --dry-run=client -o yaml | kubectl apply -f -

rm -rf "${TMP}"
echo ">> Listo. Secret 'circleguard-tls' disponible en ${NS}."
