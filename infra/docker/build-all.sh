#!/usr/bin/env bash
# Build de imágenes Docker para los 6 microservicios del Taller 2.
# Uso (Linux/Jenkins agent):  TAG=dev ./infra/docker/build-all.sh
set -euo pipefail

TAG="${TAG:-dev}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

services=(
  "circleguard-identity-service:circleguard/identity"
  "circleguard-auth-service:circleguard/auth"
  "circleguard-gateway-service:circleguard/gateway"
  "circleguard-promotion-service:circleguard/promotion"
  "circleguard-notification-service:circleguard/notification"
  "circleguard-dashboard-service:circleguard/dashboard"
)

# Timestamp para invalidar el layer del bootJar en cada corrida del pipeline.
# Esto evita que un cache corrupto (ej. tras un cuelgue del daemon Docker)
# se reuse y se quede con un build/libs/*.jar de 0 bytes.
CACHEBUST="${CACHEBUST:-$(date +%s)}"

for entry in "${services[@]}"; do
  name="${entry%%:*}"
  image="${entry##*:}"
  echo "==> Building ${image}:${TAG} (cachebust=${CACHEBUST})"
  docker build \
    -f "${ROOT}/infra/docker/service.Dockerfile" \
    --build-arg SERVICE_NAME="${name}" \
    --build-arg CACHEBUST="${CACHEBUST}" \
    -t "${image}:${TAG}" \
    "${ROOT}"
done

echo
echo "Imágenes construidas:"
docker images --filter "reference=circleguard/*:${TAG}" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
