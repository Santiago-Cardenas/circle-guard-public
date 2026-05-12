# Estrategia de ramas — Taller 2

| Rama | Entorno K8s | Pipeline | Propósito |
|---|---|---|---|
| `dev` | `circleguard-dev` | `Jenkinsfile.dev` | Integración continua diaria. Build + unit tests + deploy efímero. |
| `stage` | `circleguard-stage` | `Jenkinsfile.stage` | Validación pre-prod. Suma integration + E2E sobre K8s. |
| `master` | `circleguard-prod` | `Jenkinsfile.master` | Producción. Despliegue + Release Notes automáticas + tag. |

## Flujo

```
feature/* ──► dev ──► stage ──► master
                │        │         │
                ▼        ▼         ▼
            DEV-ns   STAGE-ns   PROD-ns
```

- Merge `dev → stage` solo si pipeline DEV está verde.
- Merge `stage → master` solo si pipeline STAGE (incluye E2E) está verde.
- `master` dispara el pipeline de release.
