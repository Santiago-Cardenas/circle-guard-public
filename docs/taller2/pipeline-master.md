# Pipeline MASTER

Es el pipeline de **liberación**. Recibe el commit que stage validó y un
operador aprobó, lo despliega al namespace `circleguard-prod` con NodePorts
en el rango `32xxx`, re-corre la suite E2E como verificación pre-release,
genera unas Release Notes automáticas y publica un *git tag* + un
*GitHub Release* con esas notas.

## Archivo

[`pipelines/Jenkinsfile.master`](../../pipelines/Jenkinsfile.master) —
Declarative Pipeline (Groovy DSL).

## Stages

| # | Stage | Acción |
|---|---|---|
| 1 | **Checkout** | Clona la rama `master` desde GitHub |
| 2 | **Toolchain check** | Verifica `docker`, `kubectl`, cluster |
| 3 | **Verify stage health** | Aborta si quedan deployments not-ready en `circleguard-stage` |
| 4 | **Tests & Build** | `./gradlew test bootJar` (excluye E2E) + publica JUnit |
| 5 | **Build images** | Etiqueta como `circleguard/*:prod` |
| 6 | **Deploy to circleguard-prod** | `apply -k infra/k8s/prod` + `rollout status` + `rollout restart` + `rollout status` |
| 7 | **Smoke test** | 6 deployments Ready + `exec` retry contra el gateway |
| 8 | **E2E tests prod** | `./gradlew :tests:e2e:test` apuntando a NodePorts `32xxx` y publica JUnit |
| 9 | **Generate Release Notes** | Crea `build/release/RELEASE-NOTES.md` y `VERSION`, los archiva como artefactos |
| 10 | **Publish release** | `git tag -a vYYYY.MM.DD-N` + push del tag + `POST` a la API de GitHub creando el Release con el cuerpo del markdown |

## Verificación previa de `stage`

Antes de tocar producción, el pipeline pregunta al cluster cuántos
deployments hay en `circleguard-stage` que aún no estén Ready. Si el
contador es mayor a cero, el build se aborta. Esto evita liberar mientras
stage está dañado o en medio de un rollout.

## Despliegue a `circleguard-prod`

El overlay [`infra/k8s/prod`](../../infra/k8s/prod) hace dos cosas:

1. **NodePorts dedicados** (rango `32xxx`) en
   [`nodeports-patch.yaml`](../../infra/k8s/prod/nodeports-patch.yaml):

   | Servicio | NodePort | URL desde el host |
   |---|---|---|
   | `gateway` | `32087` | `http://localhost:32087` |
   | `auth` | `32180` | `http://localhost:32180` |
   | `dashboard` | `32084` | `http://localhost:32084` |

2. **Réplicas dobladas** (`replicas: 2`) para `auth`, `gateway` y `dashboard`
   mediante *strategic patches* en `kustomization.yaml`. El resto de
   microservicios queda en `replicas: 1` heredado de la base.

El `apply -k` se hace antes del `rollout restart` para garantizar que el
ReplicaSet inicial converja en namespaces recién creados (mismo aprendizaje
que stage: si se hacen en paralelo aparece un race entre dos ReplicaSets).

## Release Notes automáticas

El stage **Generate Release Notes** genera dos artefactos:

- `build/release/VERSION` — solo la cadena `vYYYY.MM.DD-N` (`N` = número de
  build).
- `build/release/RELEASE-NOTES.md` — markdown con:
  - Versión, fecha UTC, build, SHA y rama.
  - Lista de las 6 imágenes Docker desplegadas.
  - Lista de commits desde el último tag `v*` hasta `HEAD`
    (`git log PREV_TAG..HEAD --pretty=format:'- %h %s (%an)' --no-merges`).
    Si es la primera release, incluye toda la historia.
  - Checklist de las verificaciones que el pipeline ya ejecutó (tests, build,
    deploy, smoke, E2E).

Ambos quedan archivados en el build con `archiveArtifacts`, así que se
pueden descargar desde la página de Jenkins.

Ver [`release-notes.md`](release-notes.md) para el formato detallado.

## Publicación: tag + GitHub Release

El stage **Publish release** hace dos cosas con la credencial Jenkins
`github-pat`:

1. **Git tag anotado**: `git tag -a vYYYY.MM.DD-N -m "Release ... desde build #N"`
   y `git push` del tag al remoto.
2. **GitHub Release**: `POST /repos/{owner}/{repo}/releases` a la API REST
   de GitHub con el JSON `{ tag_name, name, body, draft:false, prerelease:false }`.
   El `body` se construye con `python3 -c 'import json; print(json.dumps(open(...).read()))'`
   para escapar correctamente saltos de línea y comillas.

Códigos esperados:

- `201 Created` → release publicado.
- `422 Unprocessable Entity` → ya existía (re-run del mismo build); se
  loguea y continúa.
- Cualquier otro → falla el stage volcando la respuesta.

## Trigger

`pollSCM('* * * * *')`. Disparado por la auto-promoción de `circleguard-stage`
tras el `input` aprobado.

## Job en Jenkins

`http://localhost:8080/job/circleguard-master/`.
