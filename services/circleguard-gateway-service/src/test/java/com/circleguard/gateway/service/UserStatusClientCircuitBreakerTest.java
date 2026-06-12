package com.circleguard.gateway.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Prueba que simula la caida de Redis y verifica que el Circuit Breaker
 * se abre y que la llamada cae al metodo de respaldo (fallback).
 *
 * Bajamos la ventana a 5 llamadas para que el circuito abra rapido.
 */
@SpringBootTest(properties = {
        "resilience4j.circuitbreaker.instances.redisStatus.sliding-window-size=5",
        "resilience4j.circuitbreaker.instances.redisStatus.minimum-number-of-calls=5",
        "management.health.redis.enabled=false"
})
@DisplayName("UserStatusClient - Circuit Breaker")
class UserStatusClientCircuitBreakerTest {

    @Autowired
    private UserStatusClient userStatusClient;

    @Autowired
    private CircuitBreakerRegistry registry;

    // Reemplazamos Redis por un mock que siempre falla.
    @MockBean
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("Si Redis falla varias veces, el circuito se abre y usa el fallback")
    void circuitoSeAbreCuandoRedisFalla() {
        // Cualquier consulta a Redis lanza error.
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis caido"));

        CircuitBreaker cb = registry.circuitBreaker("redisStatus");

        // Llamamos varias veces; el fallback siempre devuelve UNKNOWN.
        for (int i = 0; i < 6; i++) {
            String status = userStatusClient.getUserStatus("user-1");
            assertEquals(UserStatusClient.STATUS_UNKNOWN, status);
        }

        // Despues de tantos fallos el circuito debe quedar abierto.
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    }
}
