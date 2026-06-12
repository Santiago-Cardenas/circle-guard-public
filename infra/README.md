# Infrastructure

Carpeta para artefactos de infraestructura del proyecto.

```
infra/
├── docker/        # Dockerfiles compartidos / overrides docker-compose
└── k8s/
    ├── base/      # Manifiestos base (kustomize) por servicio
    ├── dev/       # Overlays para namespace circleguard-dev
    ├── stage/     # Overlays para namespace circleguard-stage
    └── prod/      # Overlays para namespace circleguard-prod
```
