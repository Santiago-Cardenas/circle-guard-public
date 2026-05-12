# Pruebas de carga — CircleGuard

Suite [Locust](https://locust.io) que ejerce los endpoints públicos del cluster
CircleGuard (gateway, auth y dashboard expuestos por NodePort).

## Requisitos

- Python 3.10+
- `pip install locust`

## Ejecución

### Modo UI (interactivo, http://localhost:8089)

```bash
locust -f tests/perf/locustfile.py
```

Luego en el navegador abrir `http://localhost:8089` y configurar:

- **Number of users**: 50
- **Spawn rate**: 5 / s
- **Host**: `http://localhost:30087` (gateway)

### Modo headless (CI / pipeline STAGE)

```bash
locust -f tests/perf/locustfile.py \
       --headless \
       --users 50 \
       --spawn-rate 5 \
       --run-time 1m \
       --host http://localhost:30087 \
       --csv tests/perf/results
```

Genera `results_stats.csv`, `results_failures.csv`, etc. para análisis.

## Variables de entorno

| Variable        | Default                        | Uso                                   |
|-----------------|--------------------------------|---------------------------------------|
| `GATEWAY_URL`   | `http://localhost:30087`       | Override del host del gateway         |
| `AUTH_URL`      | `http://localhost:30180`       | Servicio de autenticación             |
| `DASHBOARD_URL` | `http://localhost:30084`       | Servicio de analytics                 |
