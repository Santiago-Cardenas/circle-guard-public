package com.circleguard.gateway.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas del Feature Toggle del chequeo de salud.
 * Usamos un mock del UserStatusClient para no depender de Redis.
 */
@DisplayName("QrValidationService - Feature Toggle")
class QrValidationServiceToggleTest {

    private static final String QR_SECRET = "my-qr-secret-key-for-dev-1234567890";

    // Crea un token valido firmado con el mismo secreto.
    private String tokenValido() {
        Key key = Keys.hmacShaKeyFor(QR_SECRET.getBytes());
        return Jwts.builder().setSubject("user-1").signWith(key).compact();
    }

    // Arma el servicio con el valor del toggle que queramos.
    private QrValidationService servicioCon(boolean toggle, UserStatusClient client) {
        QrValidationService service = new QrValidationService(client);
        ReflectionTestUtils.setField(service, "qrSecret", QR_SECRET);
        ReflectionTestUtils.setField(service, "healthCheckEnabled", toggle);
        return service;
    }

    @Test
    @DisplayName("Toggle apagado: deja pasar sin consultar Redis")
    void toggleApagado_noConsultaRedis() {
        UserStatusClient client = mock(UserStatusClient.class);
        QrValidationService service = servicioCon(false, client);

        var result = service.validateToken(tokenValido());

        assertTrue(result.valid());
        assertEquals("GREEN", result.status());
        // Como el toggle esta apagado, nunca se debe llamar a Redis.
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("Toggle encendido: bloquea si el estado es CONTAGIED")
    void toggleEncendido_bloqueaContagiado() {
        UserStatusClient client = mock(UserStatusClient.class);
        when(client.getUserStatus("user-1")).thenReturn("CONTAGIED");
        QrValidationService service = servicioCon(true, client);

        var result = service.validateToken(tokenValido());

        assertFalse(result.valid());
        assertEquals("RED", result.status());
    }

    @Test
    @DisplayName("Toggle encendido pero Redis fallo (UNKNOWN): deja pasar avisando")
    void toggleEncendido_estadoDesconocido() {
        UserStatusClient client = mock(UserStatusClient.class);
        when(client.getUserStatus("user-1")).thenReturn(UserStatusClient.STATUS_UNKNOWN);
        QrValidationService service = servicioCon(true, client);

        var result = service.validateToken(tokenValido());

        assertTrue(result.valid());
        assertTrue(result.message().contains("no verificado"));
    }
}
