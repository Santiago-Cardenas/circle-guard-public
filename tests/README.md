# Tests

```
tests/
├── e2e/       # REST Assured E2E sobre el stack desplegado en K8s
└── perf/      # Locust scenarios
```

Las pruebas unitarias e integración viven dentro de cada microservicio en
`services/<svc>/src/test/...`.
