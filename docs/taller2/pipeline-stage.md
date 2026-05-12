# Pipeline STAGE

Es el ambiente de **validación funcional** previo a producción. Recibe el
commit que dev acaba de validar, despliega al namespace `circleguard-stage`
con NodePorts publicados, corre la suite E2E completa contra esos NodePorts
y se detiene en un `input` manual antes de promover a `master`.

## Archivo

[`pipelines/Jenkinsfile.stage`](../../pipelines/Jenkinsfile.stage) —
Declarative Pipeline (Groovy DSL).

## Stages

| # | Stage | Acción |
|---|---|---|
| 1 | **Checkout** | Clona la rama `stage` desde GitHub |
| 2 | **Toolchain check** | Verifica `docker`, `kubectl`, cluster |
| 3 | **Tests & Build** | `./gradlew test bootJar` (excluye E2E) + publica JUnit unit/integration |
| 4 | **Build images** | Etiqueta como `circleguard/*:stage` |
| 5 | **Deploy to circleguard-stage** | `apply -k infra/k8s/stage` + `rollout status` (espera) + `rollout restart` + `rollout status` |
| 6 | **Smoke test** | 6 deployments Ready + retry de `exec` contra el gateway |
| 7 | **E2E tests** | `./gradlew :tests:e2e:test -PrunE2E=true` apuntando a los NodePorts y publica JUnit E2E |
| 8 | **Approve promotion to prod** | `input` manual con timeout de 30 min |
| 9 | **Auto-promote stage → master** | `git merge --no-ff` y push a la rama `master` |

## NodePorts publicados

El overlay [`infra/k8s/stage`](../../infra/k8s/stage) re-mapea los servicios
con NodePorts en el rango `31xxx` para que el contenedor Jenkins los pueda
alcanzar vía `host.docker.internal`:

| Servicio | NodePort | URL desde Jenkins |
|---|---|---|
| `gateway` | `31087` | `http://host.docker.internal:31087` |
| `auth` | `31180` | `http://host.docker.internal:31180` |
| `dashboard` | `31084` | `http://host.docker.internal:31084` |

El `Jenkinsfile.stage` los expone como variables de entorno
(`GATEWAY_URL`, `AUTH_URL`, `DASHBOARD_URL`) que la suite E2E consume vía
propiedades de sistema (`-DgatewayUrl=...`).

## Suite E2E

Subproyecto Gradle [`tests/e2e`](../../tests/e2e), 7 casos REST Assured que
cubren: registro/login en `auth`, listado y consulta de circuitos en
`gateway`, métricas básicas del `dashboard` y validación de errores
(HTTP 401/404). Se ejecuta con el flag `-PrunE2E=true` para que sólo corra
cuando el ambiente está disponible (en local sin cluster, queda como
`SKIPPED`).

## Gate manual a producción

```groovy
stage('Approve promotion to prod') {
  steps {
    timeout(time: 30, unit: 'MINUTES') {
      input message: 'Stage verde. Promover stage -> master (PROD)?',
            ok: 'Promover'
    }
  }
}
```

Se decidió un gate humano —no automático— para mantener control explícito
sobre qué llega a `circleguard-prod`. Si nadie aprueba en 30 min, el build
se aborta y el commit queda en stage hasta el siguiente intento.

## Auto-promoción a `master`

Mismo patrón que dev → stage (ver [`pipeline-dev.md`](pipeline-dev.md)),
sustituyendo `stage` por `master` en el `git fetch`/`git push` y dejando un
commit *"auto-promote(ci): stage -> master (build #N)"* en la rama `master`.

## Trigger

`pollSCM('* * * * *')`. Disparado normalmente por la auto-promoción del
pipeline `circleguard-dev`; también funciona con un `git push origin stage`
manual.

## Job en Jenkins

`http://localhost:8080/job/circleguard-stage/`.
