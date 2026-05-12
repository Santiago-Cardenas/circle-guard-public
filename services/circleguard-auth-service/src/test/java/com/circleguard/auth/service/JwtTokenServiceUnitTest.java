package com.circleguard.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias (M5a — Taller 2) para JwtTokenService.
 * No usan Spring; son JUnit 5 puro contra la lógica de generación
 * y firma JWT del servicio de autenticación.
 */
@DisplayName("JwtTokenService — pruebas unitarias")
class JwtTokenServiceUnitTest {

    private static final String SECRET = "una-clave-larga-y-segura-para-firmar-jwt-en-pruebas-1234567890";
    private static final long EXPIRATION_MS = 60_000L; // 1 min

    private JwtTokenService service;
    private Key signingKey;

    @BeforeEach
    void setUp() {
        service = new JwtTokenService(SECRET, EXPIRATION_MS);
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    @Test
    @DisplayName("genera un token no vacío con tres segmentos JWT (header.payload.signature)")
    void shouldGenerateNonEmptyJwtToken() {
        UUID anonId = UUID.randomUUID();
        Authentication auth = authWithAuthorities("ROLE_USER");

        String token = service.generateToken(anonId, auth);

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length, "JWT debe tener 3 segmentos");
    }

    @Test
    @DisplayName("incluye anonymousId como subject del token")
    void shouldEncodeAnonymousIdAsSubject() {
        UUID anonId = UUID.randomUUID();
        Authentication auth = authWithAuthorities("ROLE_USER");

        String token = service.generateToken(anonId, auth);

        Claims claims = parse(token);
        assertEquals(anonId.toString(), claims.getSubject());
    }

    @Test
    @DisplayName("incluye los permisos de la Authentication en el claim 'permissions'")
    void shouldIncludePermissionsClaim() {
        Authentication auth = authWithAuthorities("PERM_READ", "PERM_WRITE", "ROLE_ADMIN");

        String token = service.generateToken(UUID.randomUUID(), auth);

        Claims claims = parse(token);
        @SuppressWarnings("unchecked")
        List<String> perms = claims.get("permissions", List.class);
        assertNotNull(perms);
        assertTrue(perms.containsAll(List.of("PERM_READ", "PERM_WRITE", "ROLE_ADMIN")));
    }

    @Test
    @DisplayName("la fecha de expiración cae dentro del rango configurado")
    void shouldSetExpirationWithinConfiguredWindow() {
        long before = System.currentTimeMillis();

        String token = service.generateToken(UUID.randomUUID(), authWithAuthorities("ROLE_USER"));

        Claims claims = parse(token);
        long after = System.currentTimeMillis();
        Date exp = claims.getExpiration();
        assertNotNull(exp);
        assertTrue(exp.getTime() >= before + EXPIRATION_MS - 1_000,
                "Expiración demasiado temprana");
        assertTrue(exp.getTime() <= after + EXPIRATION_MS + 1_000,
                "Expiración demasiado tardía");
    }

    @Test
    @DisplayName("rechaza la verificación de un token firmado con otra clave")
    void shouldRejectTokenSignedWithDifferentKey() {
        String token = service.generateToken(UUID.randomUUID(), authWithAuthorities("ROLE_USER"));
        Key otherKey = Keys.hmacShaKeyFor(
                "OTRA-clave-distinta-para-pruebas-de-tamper-12345678901234".getBytes());

        assertThrows(SignatureException.class, () ->
                Jwts.parserBuilder().setSigningKey(otherKey).build().parseClaimsJws(token));
    }

    @Test
    @DisplayName("dos tokens consecutivos para el mismo sujeto son distintos (issuedAt cambia)")
    void shouldGenerateDifferentTokensOnRepeatCalls() throws InterruptedException {
        UUID anonId = UUID.randomUUID();
        Authentication auth = authWithAuthorities("ROLE_USER");

        String t1 = service.generateToken(anonId, auth);
        Thread.sleep(1_100); // forzar issuedAt distinto (resolución de segundos)
        String t2 = service.generateToken(anonId, auth);

        assertNotEquals(t1, t2);
    }

    // ---- helpers ----
    private static Authentication authWithAuthorities(String... auths) {
        return new UsernamePasswordAuthenticationToken(
                "user", "pass",
                java.util.Arrays.stream(auths).map(SimpleGrantedAuthority::new).toList());
    }

    private Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey).build()
                .parseClaimsJws(token).getBody();
    }
}
