package com.circleguard.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key;

@Service
@RequiredArgsConstructor
public class QrValidationService {

    // Cliente que consulta el estado en Redis (protegido con Circuit Breaker).
    private final UserStatusClient userStatusClient;

    @Value("${qr.secret}")
    private String qrSecret;

    // Feature Toggle: prende o apaga el chequeo de salud del usuario.
    // Por defecto esta encendido. Se puede cambiar desde el ConfigMap
    // (variable FEATURES_HEALTH_CHECK_ENABLED) sin recompilar la imagen.
    @Value("${features.health-check-enabled:true}")
    private boolean healthCheckEnabled;

    public ValidationResult validateToken(String token) {
        String anonymousId;

        // 1. Validamos la firma del token. Si esta mal, no dejamos entrar.
        try {
            Key key = Keys.hmacShaKeyFor(qrSecret.getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            anonymousId = claims.getSubject();
        } catch (Exception e) {
            return new ValidationResult(false, "RED", "Invalid or Expired Token");
        }

        // 2. Feature Toggle: si el chequeo de salud esta apagado,
        // dejamos pasar a cualquiera con token valido.
        if (!healthCheckEnabled) {
            return new ValidationResult(true, "GREEN", "Welcome to Campus");
        }

        // 3. Consultamos el estado de salud en Redis (con Circuit Breaker).
        String status = userStatusClient.getUserStatus(anonymousId);

        if ("CONTAGIED".equals(status) || "POTENTIAL".equals(status)) {
            return new ValidationResult(false, "RED", "Access Denied: Health Risk Detected");
        }

        // Si el circuito esta abierto o Redis fallo, llega "UNKNOWN".
        // En ese caso dejamos pasar pero avisamos que no se verifico.
        if (UserStatusClient.STATUS_UNKNOWN.equals(status)) {
            return new ValidationResult(true, "GREEN", "Welcome (estado de salud no verificado)");
        }

        return new ValidationResult(true, "GREEN", "Welcome to Campus");
    }

    public record ValidationResult(boolean valid, String status, String message) {}
}

