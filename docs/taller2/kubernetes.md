# Manifiestos Kubernetes (Kustomize)

## Estructura

```
infra/k8s/
├── namespaces.yaml         # circleguard-{dev,stage,prod}
├── base/                   # Manifiestos base aplicados a los 3 entornos
│   ├── kustomization.yaml
│   ├── 00-config.yaml      # ConfigMap + Secret compartidos
│   ├── 10-databases.yaml   # Postgres, Neo4j, Redis, OpenLDAP, MailHog
│   ├── 20-kafka.yaml       # Zookeeper + Kafka (cp-kafka 7.6.0)
│   └── 30-services.yaml    # Los 6 microservicios CircleGuard
└── dev/  stage/  prod/     # Overlays (namespace + tag de imagen)
    └── kustomization.yaml
```

## Desplegar

```powershell
kubectl apply -k infra/k8s/dev      # → circleguard-dev
kubectl apply -k infra/k8s/stage    # → circleguard-stage
kubectl apply -k infra/k8s/prod     # → circleguard-prod
```

## Verificación (entorno dev — verificado)

```
NAME                            READY   STATUS    RESTARTS
auth-...                        1/1     Running   0
dashboard-...                   1/1     Running   0
gateway-...                     1/1     Running   0
identity-...                    1/1     Running   0
kafka-...                       1/1     Running   0
mailhog-...                     1/1     Running   0
neo4j-...                       1/1     Running   0
notification-...                1/1     Running   0
openldap-...                    1/1     Running   0
postgres-...                    1/1     Running   0
promotion-...                   1/1     Running   1   # esperaba a Kafka
redis-...                       1/1     Running   0
zookeeper-...                   1/1     Running   0
```

13 / 13 pods `Running`. Respuesta HTTP confirmada en el puerto del Auth Service.

## Decisiones técnicas

- **Sin registry remoto**: las imágenes locales `circleguard/*:dev` se consumen
  directamente con `imagePullPolicy: IfNotPresent` (Docker Desktop comparte
  imágenes con su K8s embebido). Cero costo, sin push/pull.
- **`enableServiceLinks: false`** en TODOS los pods. K8s, por defecto, inyecta
  variables de entorno con prefijo `<SERVICE_NAME>_*` por cada Service del
  namespace. Esto rompe Neo4j (interpreta `NEO4J_PORT_7687_TCP_PORT` como
  configuración) y Kafka (`KAFKA_PORT` como override). Deshabilitarlas es la
  fix limpia.
- **MailHog** sustituye al SMTP real. Notification puede enviar emails sin
  configuración externa, y se inspeccionan en su UI (`port 8025`).
- **Config y secrets centralizados** en un único ConfigMap (`circleguard-config`)
  y un único Secret (`circleguard-secrets`). Los servicios consumen ambos vía
  `valueFrom`. JWT, QR y vault usan los mismos valores que `application.yml`
  para no romper firmas existentes.
- **Sobrescritura de URLs hardcoded** mediante variables de entorno Spring
  (`SPRING_DATASOURCE_URL`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`,
  `SPRING_DATA_REDIS_HOST`, `SPRING_LDAP_URLS`, `SPRING_NEO4J_URI`...).
  No hace falta tocar el código de los servicios.
- **Gateway expuesto como `NodePort` 30087** para pruebas E2E externas
  (`http://localhost:30087`). Los demás servicios son `ClusterIP`.

## Diferencias entre overlays

| Overlay | Namespace | Tag imágenes | Réplicas |
|---|---|---|---|
| `dev` | `circleguard-dev` | `:dev` | 1 c/u |
| `stage` | `circleguard-stage` | `:stage` | 1 c/u |
| `prod` | `circleguard-prod` | `:prod` | 2 en `auth`, `gateway`, `dashboard` |

Los pipelines (dev/stage/master) construirán y reetiquetarán las mismas imágenes
con el tag adecuado y aplicarán el overlay correspondiente.
