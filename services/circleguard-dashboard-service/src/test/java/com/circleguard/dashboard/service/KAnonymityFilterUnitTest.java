package com.circleguard.dashboard.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias (M5a — Taller 2) para KAnonymityFilter.
 * Validan el enmascaramiento k-anónimo (FR-23, Story 7.5).
 */
@DisplayName("KAnonymityFilter — pruebas unitarias")
class KAnonymityFilterUnitTest {

    private final KAnonymityFilter filter = new KAnonymityFilter();

    @Test
    @DisplayName("devuelve mapa vacío cuando la entrada es null")
    void shouldReturnEmptyMapWhenStatsNull() {
        Map<String, Object> result = filter.apply(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("enmascara totalUsers y oculta detalles cuando población < K (K=5 por defecto)")
    void shouldMaskWholeResultWhenTotalBelowK() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 3);
        stats.put("department", "Ingeniería");
        stats.put("infectedCount", 1);

        Map<String, Object> filtered = filter.apply(stats);

        assertEquals("<5", filtered.get("totalUsers"));
        assertEquals("Insufficient data for privacy", filtered.get("note"));
        assertEquals("Ingeniería", filtered.get("department"));
        assertFalse(filtered.containsKey("infectedCount"),
                "los counts no deben revelarse cuando la población es <K");
    }

    @Test
    @DisplayName("enmascara conteos individuales <K cuando la población total es >=K")
    void shouldMaskIndividualCountFieldsBelowK() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 100);
        stats.put("infectedCount", 2); // <5 → debe enmascararse
        stats.put("recoveredCount", 80); // >=5 → se mantiene
        stats.put("department", "Salud");

        Map<String, Object> filtered = filter.apply(stats);

        assertEquals("<5", filtered.get("infectedCount"));
        assertEquals(80, filtered.get("recoveredCount"));
        assertEquals(100, filtered.get("totalUsers"));
        assertEquals("Salud", filtered.get("department"));
    }

    @Test
    @DisplayName("respeta un K personalizado pasado por argumento")
    void shouldRespectCustomKValue() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 100);
        stats.put("infectedCount", 7); // <10 → debe enmascararse con K=10

        Map<String, Object> filtered = filter.apply(stats, 10);

        assertEquals("<10", filtered.get("infectedCount"));
    }

    @Test
    @DisplayName("ignora campos cuyo nombre NO termina en 'Count'")
    void shouldNotMaskFieldsThatDoNotEndInCount() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 100);
        stats.put("infected", 1); // sin sufijo Count → no toca
        stats.put("riskScore", 0.42);

        Map<String, Object> filtered = filter.apply(stats);

        assertEquals(1, filtered.get("infected"));
        assertEquals(0.42, filtered.get("riskScore"));
    }

    @Test
    @DisplayName("no enmascara conteos en cero (no hay riesgo de identificación)")
    void shouldNotMaskZeroCounts() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 100);
        stats.put("infectedCount", 0);

        Map<String, Object> filtered = filter.apply(stats);

        assertEquals(0, filtered.get("infectedCount"),
                "0 no debe enmascararse — no expone individuos");
    }

    @Test
    @DisplayName("conserva el orden de inserción del mapa")
    void shouldPreserveInsertionOrder() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", 100);
        stats.put("aaaCount", 50);
        stats.put("zzzCount", 50);

        Map<String, Object> filtered = filter.apply(stats);

        assertEquals(java.util.List.of("totalUsers", "aaaCount", "zzzCount"),
                java.util.List.copyOf(filtered.keySet()));
    }
}
