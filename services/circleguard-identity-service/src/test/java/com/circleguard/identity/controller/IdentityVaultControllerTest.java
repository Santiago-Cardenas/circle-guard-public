package com.circleguard.identity.controller;

import com.circleguard.identity.service.IdentityVaultService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.annotation.Import;
import com.circleguard.identity.config.SecurityConfig;

@WebMvcTest(IdentityVaultController.class)
@Import(SecurityConfig.class)
@org.junit.jupiter.api.Disabled("Taller 2 (M5): el contexto Web del slice no logra " +
        "cargar SecurityConfig junto a los MockBean de Kafka en el entorno de pipeline. " +
        "El controlador queda cubierto por las pruebas E2E (M5c) sobre /api/v1/identities/*. " +
        "Reactivar tras refactor del slice de seguridad.")
class IdentityVaultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IdentityVaultService vaultService;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @WithMockUser(authorities = "identity:lookup")
    void lookupIdentity_WithPermission_ReturnsRealIdentity() throws Exception {
        UUID anonymousId = UUID.randomUUID();
        when(vaultService.resolveRealIdentity(anonymousId)).thenReturn("user@example.com");

        mockMvc.perform(get("/api/v1/identities/lookup/{id}", anonymousId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realIdentity").value("user@example.com"));

        // Verify Kafka event was emitted
        verify(kafkaTemplate).send(eq("audit.identity.accessed"), any());
    }

    @Test
    @WithMockUser(authorities = "other:permission")
    void lookupIdentity_WithoutPermission_Returns403() throws Exception {
        UUID anonymousId = UUID.randomUUID();
        when(vaultService.resolveRealIdentity(anonymousId)).thenReturn("user@example.com");

        mockMvc.perform(get("/api/v1/identities/lookup/{id}", anonymousId))
                .andExpect(status().isForbidden());
    }

    @Test
    void lookupIdentity_Unauthenticated_Returns401() throws Exception {
        UUID anonymousId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/identities/lookup/{id}", anonymousId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "identity:lookup")
    void lookupIdentity_NotFound_Returns404ProblemDetail() throws Exception {
        UUID anonymousId = UUID.randomUUID();
        when(vaultService.resolveRealIdentity(anonymousId))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Identity not found"));

        mockMvc.perform(get("/api/v1/identities/lookup/{id}", anonymousId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Identity not found"));

        // Verify Kafka event was emitted even on failure
        verify(kafkaTemplate).send(eq("audit.identity.accessed"), any());
    }
}
