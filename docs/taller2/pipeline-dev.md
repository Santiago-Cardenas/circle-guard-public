# Pipeline DEV

Es la primera línea de defensa de la cadena de promoción
`dev -> stage -> master`. Optimiza para feedback rápido (~3 min): compila,
prueba, despliega al namespace `circleguard-dev` y, si todo queda verde,
promueve automáticamente el commit a la rama `stage`.

## Archivo

[`pipelines/Jenkinsfile.dev`](../../pipelines/Jenkinsfile.dev) — Declarative
Pipeline (Groovy DSL).

## Stages

| # | Stage | Acción |
|---|---|---|
| 1 | **Checkout** | Clona la rama `dev` desde GitHub |
| 2 | **Toolchain check** | Verifica `docker`, `kubectl`, conexión al cluster |
| 3 | **Tests & Build** | `./gradlew test bootJar` (excluye E2E) + publica JUnit |
| 4 | **Build images** | Ejecuta `infra/docker/build-all.sh` → 6 imágenes `circleguard/*:dev` |
| 5 | **Deploy to circleguard-dev** | `kubectl apply -k infra/k8s/dev` + `rollout restart` + `rollout status` |
| 6 | **Smoke test** | Verifica replicas Ready en los 6 deployments + HTTP al gateway |
| 7 | **Auto-promote dev → stage** | Hace `git merge --no-ff` del commit recién verificado en la rama `stage` y la pushea |

`post.failure` vuelca `kubectl get pods` y los últimos 30 eventos para diagnóstico.

## Auto-promoción a `stage`

El último stage usa la credencial Jenkins `github-pat` (Personal Access Token con
`Contents: Read and write`) para abrir un push HTTPS contra GitHub:

```sh
REMOTE_URL="https://${GH_USER}:${GH_TOKEN}@github.com/.../circle-guard-public.git"
git fetch ${REMOTE_URL} stage
git checkout -b stage_local FETCH_HEAD
git merge --no-ff ${GIT_COMMIT} -m "auto-promote(ci): dev -> stage (build #${BUILD_NUMBER})"
git push ${REMOTE_URL} stage_local:stage
```

Detalles importantes:

- Antes del `fetch` se hace `git reset --hard ${GIT_COMMIT} && git clean -fd`
  para limpiar cambios residuales del workspace (por ejemplo el bit ejecutable
  que `chmod +x` agregó a los scripts en stages anteriores). Sin esto, el
  `git checkout -b` falla con *"Your local changes would be overwritten"*.
- La rama local `stage_local` se borra y recrea en cada build para evitar
  rechazos *non-fast-forward* heredados de un build previo.
- `--no-ff` deja un commit explícito de promoción que sirve de marcador en la
  historia y referencia el `BUILD_NUMBER`.

Tras el push, `circleguard-stage` detectará el cambio en el siguiente ciclo de
SCM polling (≤ 1 min) y arrancará por sí solo.

## Trigger

`pollSCM('* * * * *')` — Jenkins hace polling cada minuto a la rama `dev`.
Cualquier `git push origin dev` dispara un nuevo build automáticamente.

## Job en Jenkins

Creado en `/var/jenkins_home/jobs/circleguard-dev/config.xml` (apunta al
fork público + `pipelines/Jenkinsfile.dev`). Visible en
`http://localhost:8080/job/circleguard-dev/`.

## Pre-requisitos en el container Jenkins

Instalados en el contenedor Jenkins:

- **docker CLI** (binario estático 27.3.1) en `/usr/local/bin/docker`.
- **wget** (vía apt) para health-checks.
- **Grupo docker** creado con el GID del socket; usuario `jenkins` añadido.
- `kubectl` y `KUBECONFIG=/var/jenkins_home/.kube/config` ya configurados en el setup de Jenkins.

## Cómo ejecutar manualmente

UI: `http://localhost:8080/job/circleguard-dev/` → **Build Now**.
