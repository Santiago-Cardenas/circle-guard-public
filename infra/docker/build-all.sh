#!/usr/bin/env bash
# Build de imágenes Docker para los microservicios de CircleGuard.
#
# Estrategia: los JARs se compilan una sola vez en el host (stage
# "Assemble JARs" del Jenkinsfile, vía ./gradlew bootJar). Aquí solo
# empaquetamos el JAR ya compilado en una imagen JRE minimalista.
# Esto elimina arrancar 6 JVMs Gradle dentro de Docker y evita ahogar
# Docker Desktop free tier en WSL2.
#
# Uso:  TAG=dev ./infra/docker/build-all.sh
set -euo pipefail

TAG="${TAG:-dev}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DOCKERFILE="${ROOT}/infra/docker/service.runtime.Dockerfile"

services=(
  "circleguard-identity-service:circleguard/identity"
  "circleguard-auth-service:circleguard/auth"
  "circleguard-gateway-service:circleguard/gateway"
  "circleguard-promotion-service:circleguard/promotion"
  "circleguard-notification-service:circleguard/notification"
  "circleguard-dashboard-service:circleguard/dashboard"
)

for entry in "${services[@]}"; do
  name="${entry%%:*}"
  image="${entry##*:}"

  # Spring Boot bootJar genera 2 jars: el ejecutable y *-plain.jar (~26KB de metadatos).
  # Tomamos solo el ejecutable.
  jar_path="$(find "${ROOT}/services/${name}/build/libs" -maxdepth 1 -type f \
                -name '*.jar' ! -name '*-plain.jar' | head -n 1 || true)"

  if [[ -z "${jar_path}" || ! -s "${jar_path}" ]]; then
    echo "ERROR: no se encontró un JAR ejecutable para ${name} en services/${name}/build/libs/" >&2
    echo "       Ejecuta antes: ./gradlew :services:${name}:bootJar" >&2
    exit 1
  fi

  rel_jar="${jar_path#${ROOT}/}"
  size="$(du -h "${jar_path}" | cut -f1)"
  echo "==> Building ${image}:${TAG}  (jar=${rel_jar}, ${size})"
  docker build \
    -f "${DOCKERFILE}" \
    --build-arg SERVICE_NAME="${name}" \
    --build-arg JAR_PATH="${rel_jar}" \
    -t "${image}:${TAG}" \
    "${ROOT}"
done

echo
echo "Imágenes construidas:"
docker images --filter "reference=circleguard/*:${TAG}" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
