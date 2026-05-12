# Pipeline DEV

## Archivo

[`pipelines/Jenkinsfile.dev`](../../pipelines/Jenkinsfile.dev) — Declarative
Pipeline (Groovy DSL).

## Stages

| # | Stage | Acción |
|---|---|---|
| 1 | **Checkout** | Clona la rama `dev` desde GitHub |
| 2 | **Toolchain check** | Verifica `docker`, `kubectl`, conexión al cluster |
| 3 | **Build images** | Ejecuta `infra/docker/build-all.sh` → 6 imágenes `circleguard/*:dev` |
| 4 | **Deploy to circleguard-dev** | `kubectl apply -k infra/k8s/dev` + `rollout restart` + `rollout status` |
| 5 | **Smoke test** | Verifica replicas Ready en los 6 deployments + HTTP al gateway |

`post.failure` vuelca `kubectl get pods` y los últimos 30 eventos para diagnóstico.

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
