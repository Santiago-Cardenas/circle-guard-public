# Release Notes

Las Release Notes se generan **automáticamente** en cada ejecución exitosa
del pipeline `circleguard-master`, en el stage **Generate Release Notes**.
No requieren intervención manual.

## Versionado

Formato: `vYYYY.MM.DD-N`, donde:

- `YYYY.MM.DD` es la fecha UTC del build.
- `N` es el `BUILD_NUMBER` de Jenkins (estrictamente creciente, único por
  pipeline).

Ejemplos: `v2026.05.12-2`, `v2026.05.12-3`, `v2026.05.13-1`.

Esta convención garantiza unicidad incluso cuando hay varios releases en el
mismo día y mantiene un orden cronológico legible sin depender de SemVer
(que no aplica bien a un mono-repo en evolución continua para un taller).

## Dónde verlas

Tres lugares, con la misma información:

1. **Jenkins — artefactos del build**
   `http://localhost:8080/job/circleguard-master/<N>/artifact/build/release/`
   Contiene `RELEASE-NOTES.md` y `VERSION`.

2. **GitHub — Tags**
   `https://github.com/<owner>/circle-guard-public/tags`
   Cada release queda como tag anotado con el mensaje
   `"Release vYYYY.MM.DD-N desde build #N"`.

3. **GitHub — Releases**
   `https://github.com/<owner>/circle-guard-public/releases/tag/vYYYY.MM.DD-N`
   El cuerpo es el contenido completo de `RELEASE-NOTES.md`. Publicado vía
   API REST en el stage **Publish release** del pipeline master.

## Contenido (formato)

```markdown
# Release vYYYY.MM.DD-N

**Fecha**: 2026-05-12 23:14:33 UTC
**Build**: #N
**Commit**: <sha>
**Rama**: master
**Ambiente**: circleguard-prod (NodePorts 32087/32180/32084)

## Imagenes desplegadas
- circleguard/auth:prod
- circleguard/identity:prod
- circleguard/gateway:prod
- circleguard/promotion:prod
- circleguard/notification:prod
- circleguard/dashboard:prod

## Cambios incluidos (desde vYYYY.MM.DD-(N-1) hasta HEAD)

- <sha> <subject> (<author>)
- ...

## Verificacion automatica
- [x] Tests unitarios + integracion verdes
- [x] Build de 6 imagenes Docker exitoso
- [x] Deploy a circleguard-prod sin errores
- [x] Smoke test HTTP contra gateway OK
- [x] Suite E2E (REST Assured) verde
```

## Cómo se construye el log de cambios

```sh
PREV_TAG=$(git describe --tags --abbrev=0 --match 'v*' 2>/dev/null || true)
git log ${PREV_TAG}..HEAD --pretty=format:'- %h %s (%an)' --no-merges
```

- Si existe un tag `v*` previo → diff desde ese tag hasta `HEAD`.
- Si es la primera release → toda la historia hasta `HEAD`.
- Excluye merge commits (`--no-merges`) para mantener el log centrado en el
  trabajo real, no en las promociones automáticas dev→stage→master.

## Cómo se publica el GitHub Release

```sh
curl -sS -X POST \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer ${GH_TOKEN}" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/<owner>/<repo>/releases \
  -d '{"tag_name":"vYYYY.MM.DD-N","name":"vYYYY.MM.DD-N",
       "body":"<markdown escapado>","draft":false,"prerelease":false}'
```

Token: la credencial Jenkins `github-pat` (Personal Access Token con
permiso `Contents: Read and write` sobre el repo).
