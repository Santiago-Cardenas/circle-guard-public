package com.circleguard.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias (M5a — Taller 2) para QrTokenService.
 */
@DisplayName("QrTokenService — pruebas unitarias")
class QrTokenServiceUnitTest {

    private static final String SECRET = "clave-qr-de-pruebas-suficientemente-larga-para-hs256-12345";
    private static final long SHORT_EXP = 5_000L;

    @Test
    @DisplayName("genera un JWT válido con 3 segmentos")
    void shouldGenerateThreeSegmentJwt() {
        QrTokenService svc = new QrTokenService(SECRET, SHORT_EXP);
        String token = svc.generateQrToken(UUID.randomUUID());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("subject del token es el anonymousId pasado")
    void shouldEncodeAnonymousIdAsSubject() {
        QrTokenService svc = new QrTokenService(SECRET, SHORT_EXP);
        UUID anon = UUID.randomUUID();

        String token = svc.generateQrToken(anon);

        Claims claims = parse(token);
        assertEquals(anon.toString(), claims.getSubject());
    }

    @Test
    @DisplayName("expira en la ventana corta configurada (5 s)")
    void shouldSetShortExpiration() {
        QrTokenService svc = new QrTokenService(SECRET, SHORT_EXP);
        long before = System.currentTimeMillis();

        String token = svc.generateQrToken(UUID.randomUUID());

        Date exp = parse(token).getExpiration();
        assertTrue(exp.getTime() <= before + SHORT_EXP + 1_000);
        assertTrue(exp.getTime() >= before + SHORT_EXP - 1_000);
    }

    @Test
    @DisplayName("issuedAt no es nulo y es anterior a la expiración")
    void shouldHaveIssuedAtBeforeExpiration() {
        QrTokenService svc = new QrTokenService(SECRET, SHORT_EXP);

        Claims claims = parse(svc.generateQrToken(UUID.randomUUID()));

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getIssuedAt().before(claims.getExpiration()));
    }

    @Test
    @DisplayName("el token NO se puede verificar con otra clave (firma inválida)")
    void shouldFailVerificationWithDifferentKey() {
        QrTokenService svc = new QrTokenService(SECRET, SHORT_EXP);
        String token = svc.generateQrToken(UUID.randomUUID());
        Key tampered = Keys.hmacShaKeyFor(
                "atacante-clave-falsa-1234567890123456789012345678".getBytes());

        assertThrows(io.jsonwebtoken.JwtException.class, () ->
                Jwts.parserBuilder().setSigningKey(tampered).build().parseClaimsJws(token));
    }

    private Claims parse(String token) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
    }
}
