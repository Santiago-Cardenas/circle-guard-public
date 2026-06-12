# Jenkins Pipelines

Jenkinsfiles del proyecto.

| Archivo | Entorno | Descripción |
|---|---|---|
| `Jenkinsfile.dev` | dev | Build + unit tests + SonarQube + docker build + Trivy + deploy a `circleguard-dev` |
| `Jenkinsfile.stage` | stage | Build + tests + deploy a `circleguard-stage` + E2E + OWASP ZAP (DAST) |
| `Jenkinsfile.master` | prod | Build + tests + validación de stage + deploy a `circleguard-prod` + versión semántica + Release Notes |

## Seguridad en el pipeline (Fase 4)

El pipeline incluye varias capas de seguridad y calidad:

- **SAST / calidad (SonarQube)**: en `dev` corremos `./gradlew sonar` contra el
  servidor SonarQube (http://host.docker.internal:9000). La cobertura la mide
  **JaCoCo** (reporte XML en `**/build/reports/jacoco/test/jacocoTestReport.xml`).
  Requiere una credencial secreta en Jenkins llamada `sonar-token`.
- **Escaneo de imágenes (Trivy)**: en `dev`, después de construir las imágenes,
  Trivy busca vulnerabilidades `HIGH`/`CRITICAL` en cada imagen `circleguard/*`.
  Los reportes quedan archivados en `build/security/`.
- **DAST (OWASP ZAP)**: en `stage`, tras desplegar, ZAP hace un *baseline scan*
  contra el gateway desplegado. El reporte queda en `build/security/zap-gateway.html`.

## Versionado semántico

`pipelines/semver.sh` calcula la siguiente versión (vX.Y.Z) a partir de los
commits siguiendo Conventional Commits:

- `feat!:` o `BREAKING CHANGE` → sube **MAJOR**
- `feat:` → sube **MINOR**
- `fix:` (o el resto) → sube **PATCH**

La versión base está en el archivo `VERSION` (raíz del repo) y, una vez existan
tags `vX.Y.Z`, se toma el último tag como punto de partida.

## Notificaciones de fallo

`pipelines/notify.sh` manda un correo a **Mailhog** (vía SMTP) cuando un pipeline
falla. Mailhog corre en Kubernetes y se expone por NodePort `30025` (SMTP) y
`30825` (UI), por eso desde Jenkins se alcanza en `host.docker.internal:30025`.

## Credenciales necesarias en Jenkins

| ID | Tipo | Uso |
|---|---|---|
| `github-pat` | Username + Password (PAT) | push de auto-promoción y publicar releases |
| `sonar-token` | Secret text | autenticación contra SonarQube |
