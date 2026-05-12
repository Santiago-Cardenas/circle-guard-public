# Dockerización de los 6 microservicios

## Estrategia

- **Un único Dockerfile parametrizado**: [`infra/docker/service.Dockerfile`](../../infra/docker/service.Dockerfile) recibe `--build-arg SERVICE_NAME=...` y construye cualquiera de los 6 servicios. Evita duplicación de 6 Dockerfiles casi idénticos.
- **Multi-stage build**:
  1. `eclipse-temurin:21-jdk-jammy` → ejecuta `./gradlew :services:<svc>:bootJar -x test`.
  2. `eclipse-temurin:21-jre-jammy` → solo el fat-jar resultante, ejecutado como usuario `app` no-root.
- **Healthcheck** vía `actuator/health` en :8080.
- **No se ejecutan tests durante `docker build`** (`-x test`); las pruebas las dispara Jenkins en sus etapas dedicadas (más rápido y separa responsabilidades).

## Imágenes producidas

| Servicio | Imagen | Tamaño aprox |
|---|---|---|
| circleguard-auth-service | `circleguard/auth:dev` | 510 MB |
| circleguard-identity-service | `circleguard/identity:dev` | 540 MB |
| circleguard-gateway-service | `circleguard/gateway:dev` | 471 MB |
| circleguard-promotion-service | `circleguard/promotion:dev` | 565 MB |
| circleguard-notification-service | `circleguard/notification:dev` | 524 MB |
| circleguard-dashboard-service | `circleguard/dashboard:dev` | 502 MB |

## Cómo construir todas localmente

```powershell
# PowerShell (host Windows)
./infra/docker/build-all.ps1 -Tag dev
```

```bash
# Bash (agente Jenkins / Linux)
TAG=dev ./infra/docker/build-all.sh
```

## Decisión: registry

No usamos registry remoto. Docker Desktop comparte sus imágenes locales con
su Kubernetes embebido siempre que los manifiestos usen
`imagePullPolicy: IfNotPresent`. Esto cumple los requisitos del taller con
la opción más simple y sin costo.

## Smoke test verificado

```powershell
docker run -d --name cg-smoke circleguard/auth:dev
docker logs cg-smoke
# Resultado: arranque OK hasta intentar conectar Postgres (esperado fuera del cluster)
```

El jar arranca, Spring Boot inicializa y falla solo en la conexión a la base
(externa). En K8s tendrá un Service `postgres` accesible y arrancará completo.

## `.dockerignore`

[`/.dockerignore`](../../.dockerignore) excluye `mobile/`, `docs/`, `.git/`,
`build/`, `bin/`, `tests/`, etc. Reduce el contexto enviado al daemon de varios
GB a ~37 KB, acelerando enormemente cada build incremental.
