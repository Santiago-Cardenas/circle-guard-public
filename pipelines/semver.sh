#!/usr/bin/env bash
# Calcula la siguiente version semantica (semver) mirando los commits.
#
# La idea es seguir Conventional Commits:
#   - feat!: o "BREAKING CHANGE"  -> sube el MAJOR  (1.0.0 -> 2.0.0)
#   - feat:                       -> sube el MINOR  (1.0.0 -> 1.1.0)
#   - fix: (o cualquier otra cosa)-> sube el PATCH  (1.0.0 -> 1.0.1)
#
# Toma como base el ultimo tag vX.Y.Z; si no hay tags usa el archivo VERSION.
# Imprime la nueva version (ej: v1.2.0) por stdout.
set -e

# 1) Version base: el ultimo tag semver, o el archivo VERSION si no hay tags.
LAST_TAG=$(git describe --tags --abbrev=0 --match 'v[0-9]*.[0-9]*.[0-9]*' 2>/dev/null || true)
if [ -z "${LAST_TAG}" ]; then
  BASE="$(cat VERSION 2>/dev/null || echo '1.0.0')"
  RANGE="HEAD"
else
  BASE="${LAST_TAG#v}"   # le quito la 'v' del inicio
  RANGE="${LAST_TAG}..HEAD"
fi

MAJOR=$(echo "${BASE}" | cut -d. -f1)
MINOR=$(echo "${BASE}" | cut -d. -f2)
PATCH=$(echo "${BASE}" | cut -d. -f3)

# 2) Reviso los mensajes de commit nuevos para decidir que subir.
LOG=$(git log ${RANGE} --pretty=format:'%s%n%b' --no-merges 2>/dev/null || echo "")

BUMP="patch"   # por defecto subimos patch
if echo "${LOG}" | grep -Eq 'BREAKING CHANGE|^[a-z]+(\(.+\))?!:'; then
  BUMP="major"
elif echo "${LOG}" | grep -Eq '^feat(\(.+\))?:'; then
  BUMP="minor"
elif echo "${LOG}" | grep -Eq '^fix(\(.+\))?:'; then
  BUMP="patch"
fi

# 3) Aplico el incremento.
case "${BUMP}" in
  major) MAJOR=$((MAJOR+1)); MINOR=0; PATCH=0 ;;
  minor) MINOR=$((MINOR+1)); PATCH=0 ;;
  patch) PATCH=$((PATCH+1)) ;;
esac

echo "v${MAJOR}.${MINOR}.${PATCH}"
