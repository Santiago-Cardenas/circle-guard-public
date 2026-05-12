package com.circleguard.dashboard.integration;

import com.circleguard.dashboard.controller.AnalyticsController;
import com.circleguard.dashboard.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración (M5b — Taller 2) para AnalyticsController.
 */
@WebMvcTest(AnalyticsController.class)
@DisplayName("AnalyticsController — pruebas de integración (slice MVC)")
class AnalyticsControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AnalyticsService analyticsService;

    @Test
    @DisplayName("GET /summary devuelve un mapa con total y timestamp")
    void shouldReturnCampusSummary() throws Exception {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalUsers", 1500);
        summary.put("timestamp", "2025-05-12T10:00:00Z");
        Mockito.when(analyticsService.getCampusSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/v1/analytics/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(1500))
                .andExpect(jsonPath("$.timestamp").value("2025-05-12T10:00:00Z"));
    }

    @Test
    @DisplayName("GET /department/{d} devuelve estadísticas del departamento")
    void shouldReturnDepartmentStats() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("department", "Sistemas");
        stats.put("infectedCount", 12);
        Mockito.when(analyticsService.getDepartmentStats("Sistemas")).thenReturn(stats);

        mockMvc.perform(get("/api/v1/analytics/department/Sistemas")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("Sistemas"))
                .andExpect(jsonPath("$.infectedCount").value(12));
    }

    @Test
    @DisplayName("GET /time-series con parámetros default → 200 + lista")
    void shouldReturnTimeSeriesWithDefaults() throws Exception {
        List<Map<String, Object>> series = new ArrayList<>();
        Map<String, Object> point = new HashMap<>();
        point.put("hour", 0);
        point.put("count", 5);
        series.add(point);
        Mockito.when(analyticsService.getTimeSeries(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(series);

        mockMvc.perform(get("/api/v1/analytics/time-series")
                        .param("period", "hourly")
                        .param("limit", "24")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hour").value(0))
                .andExpect(jsonPath("$[0].count").value(5));
    }

    @Test
    @DisplayName("GET /trends/{id} con UUID válido → 200")
    void shouldReturnTrendsForLocation() throws Exception {
        UUID loc = UUID.randomUUID();
        Mockito.when(analyticsService.getEntryTrends(loc))
                .thenReturn(List.of(Map.of("hour", "08:00", "count", 42)));

        mockMvc.perform(get("/api/v1/analytics/trends/" + loc)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].count").value(42));
    }

    @Test
    @DisplayName("GET /trends/{id} con UUID inválido → 400")
    void shouldRejectInvalidUuid() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/trends/no-es-uuid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /health-board → headers CORS presentes (anotación @CrossOrigin)")
    void shouldExposeCorsHeaders() throws Exception {
        Mockito.when(analyticsService.getGlobalHealthStats()).thenReturn(Map.of("totalGreen", 100));

        mockMvc.perform(get("/api/v1/analytics/health-board")
                        .header("Origin", "http://localhost:3000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}
