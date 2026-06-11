package com.circleguard.gateway.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Cliente que consulta el estado de salud del usuario en Redis.
 *
 * Lo dejamos en su propia clase para poder ponerle el Circuit Breaker.
 * Si Redis se cae o responde lento muchas veces seguidas, el circuito
 * se "abre" y en vez de quedarse esperando se llama al metodo de respaldo
 * (fallback). Asi el gateway sigue respondiendo aunque Redis falle.
 */
@Component
@RequiredArgsConstructor
public class UserStatusClient {

    private final StringRedisTemplate redisTemplate;

    // En Redis el estado se guarda con esta llave: "user:status:<id>".
    private static final String STATUS_KEY_PREFIX = "user:status:";

    // Valor que devolvemos cuando no pudimos consultar el estado real.
    public static final String STATUS_UNKNOWN = "UNKNOWN";

    /**
     * Busca el estado de salud del usuario en Redis.
     * El nombre "redisStatus" debe coincidir con el de application.yml.
     */
    @CircuitBreaker(name = "redisStatus", fallbackMethod = "statusFallback")
    public String getUserStatus(String anonymousId) {
        return redisTemplate.opsForValue().get(STATUS_KEY_PREFIX + anonymousId);
    }

    /**
     * Metodo de respaldo. Se ejecuta cuando la consulta a Redis falla
     * o cuando el circuito esta abierto. Devolvemos UNKNOWN para indicar
     * que no se pudo verificar el estado de salud.
     */
    public String statusFallback(String anonymousId, Throwable t) {
        return STATUS_UNKNOWN;
    }
}
