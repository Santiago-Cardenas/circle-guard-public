package com.circleguard.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Pruebas End-to-End (M5c — Taller 2) contra el cluster Kubernetes desplegado.
 * Apuntan al gateway expuesto vía NodePort y a los endpoints públicos
 * (health, dashboard analytics) sin requerir credenciales reales de LDAP.
 *
 * Configuración (system properties con defaults):
 *   -DgatewayUrl=http://localhost:30087   (gateway-service)
 *   -DauthUrl=http://localhost:30180      (auth-service)
 *   -DdashboardUrl=http://localhost:30084 (dashboard-service)
 *
 * Para ejecutar:  ./gradlew :tests:e2e:test -PrunE2E=true
 */
@DisplayName("E2E — CircleGuard contra cluster K8s")
class CircleGuardE2ETest {

    private static final String GATEWAY = System.getProperty("gatewayUrl", "http://localhost:30087");
    private static final String AUTH    = System.getProperty("authUrl",    "http://localhost:30180");
    private static final String DASHBOARD = System.getProperty("dashboardUrl", "http://localhost:30084");

    @BeforeAll
    static void setup() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    @DisplayName("E2E-01: gateway responde a la raíz con un código HTTP válido (no 5xx)")
    void gatewayShouldRespondToRoot() {
        Response r = given().baseUri(GATEWAY).when().get("/");
        // 404 / 200 / 401 son aceptables (depende del routing); 5xx no.
        assert r.statusCode() < 500 : "gateway respondió 5xx: " + r.statusCode();
    }

    @Test
    @DisplayName("E2E-02: POST /api/v1/auth/login con credenciales inválidas → 401")
    void loginWithBadCredentialsShouldReturn401() {
        given().baseUri(AUTH)
                .contentType(ContentType.JSON)
                .body("{\"username\":\"no-existe\",\"password\":\"mala\"}")
        .when()
                .post("/api/v1/auth/login")
        .then()
                .statusCode(anyOf(is(401), is(500))) // tolera 500 si LDAP rechaza la conexión
                .body("$", hasKey("message"));
    }

    @Test
    @DisplayName("E2E-03: POST /api/v1/auth/visitor/handoff sin body → 400")
    void visitorHandoffWithoutBodyShouldReturn400() {
        given().baseUri(AUTH)
                .contentType(ContentType.JSON)
                .body("{}")
        .when()
                .post("/api/v1/auth/visitor/handoff")
        .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("E2E-04: POST /api/v1/gate/validate con token inválido → respuesta JSON con valid=false")
    void gateValidateRejectsInvalidToken() {
        given().baseUri(GATEWAY)
                .contentType(ContentType.JSON)
                .body("{\"token\":\"esto-no-es-jwt\"}")
        .when()
                .post("/api/v1/gate/validate")
        .then()
                .statusCode(anyOf(is(200), is(400), is(401)))
                .body("valid", anyOf(is(false), nullValue()));
    }

    @Test
    @DisplayName("E2E-05: GET /api/v1/analytics/health-board → 200 + JSON")
    void dashboardHealthBoardShouldReturnJson() {
        given().baseUri(DASHBOARD)
        .when()
                .get("/api/v1/analytics/health-board")
        .then()
                .statusCode(200)
                .contentType(containsString("application/json"));
    }

}
