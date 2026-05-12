# Configuración Jenkins / Docker / Kubernetes

## Estado verificado

| Componente | Estado |
|---|---|
| Docker Desktop | OK (engine running) |
| Kubernetes (docker-desktop) | OK, 1 nodo, v1.34.1 |
| Namespaces `circleguard-{dev,stage,prod}` | Creados (`infra/k8s/namespaces.yaml`) |
| Jenkins container `goofy_kilby` (`jenkins/jenkins:lts`) | Up, puertos 8080 + 50000 |
| Docker socket montado en Jenkins | OK (`/run/host-services/docker.proxy.sock` → `/var/run/docker.sock`) |
| Docker CLI dentro de Jenkins | OK |
| kubectl dentro de Jenkins | Instalado (`/usr/local/bin/kubectl`, v1.34.1) |
| python3 + pip3 dentro de Jenkins | Instalado (Python 3.13.5) — necesario para Locust |
| kubeconfig dentro de Jenkins | `/var/jenkins_home/.kube/config` |
| Conectividad Jenkins → K8s API | OK vía `kubernetes.docker.internal` (mapeada a `172.17.0.1`) |

## ⚠️ Persistencia del mapeo de `/etc/hosts`

El kubeconfig apunta a `https://kubernetes.docker.internal:6443`. Esa entrada se
añadió manualmente a `/etc/hosts` del container y **se perderá si recreas el
container**. Para hacerlo permanente, recrea Jenkins con `--add-host`:

```powershell
# 1) Detener y eliminar el container actual (jenkins_home se preserva, es un volumen)
docker stop goofy_kilby
docker rm   goofy_kilby

# 2) Recrear con add-host y los mismos volúmenes/puertos
docker run -d --name jenkins `
  --restart unless-stopped `
  -p 8080:8080 -p 50000:50000 `
  -v jenkins_home:/var/jenkins_home `
  -v /run/host-services/docker.proxy.sock:/var/run/docker.sock `
  --add-host=kubernetes.docker.internal:host-gateway `
  --add-host=host.docker.internal:host-gateway `
  jenkins/jenkins:lts

# 3) Reinstalar tooling (kubectl + python3) — script idempotente:
docker exec -u root jenkins bash -c "`
  apt-get update -qq && `
  apt-get install -y -qq curl python3 python3-pip python3-venv >/dev/null && `
  curl -sLo /usr/local/bin/kubectl https://dl.k8s.io/release/v1.34.1/bin/linux/amd64/kubectl && `
  chmod +x /usr/local/bin/kubectl"
```

> Alternativa: construir un `Dockerfile` propio basado en `jenkins/jenkins:lts`
> con kubectl + python3 baked-in. Lo dejamos como mejora futura; para el taller
> el approach anterior es suficiente.

## Plugins requeridos en Jenkins (instalación manual)

Manage Jenkins → Plugins → Available:

**Pipeline & SCM**
- Pipeline (`workflow-aggregator`)
- Pipeline: Stage View
- Pipeline Utility Steps
- Blue Ocean
- Git
- GitHub Branch Source
- GitHub Integration

**Build**
- Gradle
- Eclipse Temurin Installer (JDK 21 auto-install)

**Docker**
- Docker Pipeline
- Docker Commons

**Kubernetes**
- Kubernetes CLI
- Kubernetes Credentials

**Calidad / reporting**
- JUnit
- JaCoCo
- HTML Publisher (para reportes Locust)
- Test Results Analyzer
- Warnings Next Generation

**Otros**
- Credentials Binding
- AnsiColor
- Timestamper
- Build Timeout
- Workspace Cleanup
- Email Extension
- Generic Webhook Trigger

## Credenciales a crear (Manage Jenkins → Credentials → System → Global)

| ID | Tipo | Uso |
|---|---|---|
| `github-creds` | Username + Password (PAT) | Checkout del repo (también permite tags/releases) |
| `kubeconfig-docker-desktop` | Secret file | Subir `/var/jenkins_home/.kube/config` (o el local del host) — usado por `withKubeConfig` |

> No usamos registry remoto: Docker Desktop comparte sus imágenes con su
> Kubernetes embebido, por lo que `imagePullPolicy: IfNotPresent` basta.

## Validaciones rápidas

```powershell
# Desde el host
kubectl get ns | Select-String circleguard

# Desde el container Jenkins
docker exec goofy_kilby bash -c "docker version --format '{{.Server.Version}}'; kubectl get ns | grep circleguard; python3 --version"
```
