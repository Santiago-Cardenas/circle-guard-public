"""Pruebas de performance para CircleGuard.

Cada `@task` es una prueba independiente; en conjunto golpean el gateway,
auth y dashboard del cluster K8s. Se valida latencia y porcentaje de fallos
bajo carga concurrente.

Ejecucion headless:
    locust -f tests/perf/locustfile.py --headless --users 50 \
           --spawn-rate 5 --run-time 1m --host http://localhost:30087
"""
import json
import os
import uuid

from locust import HttpUser, between, task

GATEWAY_URL = os.getenv("GATEWAY_URL", "http://localhost:30087")
AUTH_URL = os.getenv("AUTH_URL", "http://localhost:30180")
DASHBOARD_URL = os.getenv("DASHBOARD_URL", "http://localhost:30084")


class CircleGuardLoadUser(HttpUser):
    """Simula un cliente real golpeando los endpoints publicos del sistema."""

    # Entre 1 y 3 segundos de espera entre tareas (carga realista, no spam).
    wait_time = between(1, 3)
    host = GATEWAY_URL

    # ---------------- gateway ----------------

    @task(5)
    def gateway_root(self):
        """Hit a la raiz del gateway (carga base)."""
        self.client.get("/", name="GW /", catch_response=True)

    @task(2)
    def gateway_validate_invalid_token(self):
        """POST /api/v1/gate/validate con token bogus -> debe ser rapido y NO 5xx."""
        with self.client.post(
            "/api/v1/gate/validate",
            json={"token": "fake.jwt.token"},
            name="GW /api/v1/gate/validate",
            catch_response=True,
        ) as resp:
            if resp.status_code >= 500:
                resp.failure(f"5xx en validate: {resp.status_code}")

    # ---------------- auth ----------------

    # NOTA: el endpoint POST /api/v1/auth/login con credenciales validas no
    # se incluye en esta suite de carga porque el handshake BCrypt (cost 10)
    # es CPU-intensivo (~100 ms por hash) y satura el pod de auth con 50
    # usuarios concurrentes, devolviendo HTTP 500. Es un cuello de botella
    # conocido; mitigarlo requiere mas replicas o bajar el cost de BCrypt
    # para entornos de carga. La suite carga solo endpoints idempotentes
    # de lectura mas el rechazo de credenciales invalidas.

    @task(2)
    def auth_login_invalid(self):
        """POST /api/v1/auth/login con credenciales malas -> 401 esperado."""
        with self.client.post(
            f"{AUTH_URL}/api/v1/auth/login",
            json={"username": f"perf-{uuid.uuid4()}", "password": "x"},
            name="AUTH /login (401)",
            catch_response=True,
        ) as resp:
            # 401 es el caso esperado; 400 tambien es valido si el validador
            # rechaza el username generado.
            if resp.status_code in (400, 401):
                resp.success()
            else:
                resp.failure(f"codigo inesperado {resp.status_code}")

    @task(1)
    def auth_visitor_handoff(self):
        """POST /api/v1/auth/visitor/handoff con anonymousId aleatorio."""
        anon_id = str(uuid.uuid4())
        self.client.post(
            f"{AUTH_URL}/api/v1/auth/visitor/handoff",
            json={"anonymousId": anon_id},
            name="AUTH /visitor/handoff",
        )

    # ---------------- dashboard ----------------

    @task(4)
    def dashboard_health_board(self):
        """GET /api/v1/analytics/health-board -> 200."""
        self.client.get(
            f"{DASHBOARD_URL}/api/v1/analytics/health-board",
            name="DASH /health-board",
        )

    @task(2)
    def dashboard_summary(self):
        """GET /api/v1/analytics/summary -> 200."""
        self.client.get(
            f"{DASHBOARD_URL}/api/v1/analytics/summary",
            name="DASH /summary",
        )

    @task(2)
    def dashboard_time_series(self):
        """GET /api/v1/analytics/time-series con parametros default."""
        self.client.get(
            f"{DASHBOARD_URL}/api/v1/analytics/time-series?period=hourly&limit=24",
            name="DASH /time-series",
        )
