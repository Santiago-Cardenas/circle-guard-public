# Jenkins Pipelines

Jenkinsfiles del Taller 2.

| Archivo | Entorno | Descripción |
|---|---|---|
| `Jenkinsfile.dev` | dev | Build + unit tests + docker build + deploy a `circleguard-dev` |
| `Jenkinsfile.stage` | stage | Build + unit + integration + deploy a `circleguard-stage` + E2E sobre K8s |
| `Jenkinsfile.master` | prod | Build + tests + validación de stage + deploy a `circleguard-prod` + Release Notes automáticas |

Las definiciones se materializan en hitos M4, M6 y M7.
