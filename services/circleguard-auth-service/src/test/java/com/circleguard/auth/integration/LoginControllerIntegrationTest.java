package com.circleguard.auth.integration;

import com.circleguard.auth.client.IdentityClient;
import com.circleguard.auth.controller.LoginController;
import com.circleguard.auth.security.SecurityConfig;
import com.circleguard.auth.service.CustomUserDetailsService;
import com.circleguard.auth.service.JwtTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración para LoginController.
 * Levanta el slice MVC con la SecurityConfig real; mockea
 * únicamente las dependencias externas (auth manager, identity client).
 */
@WebMvcTest(LoginController.class)
@Import(SecurityConfig.class)
@DisplayName("LoginController — pruebas de integración (slice MVC + Security)")
class LoginControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AuthenticationManager authManager;
    @MockBean private JwtTokenService jwtService;
    @MockBean private IdentityClient identityClient;
    @MockBean private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /login con credenciales válidas → 200 + token JWT + anonymousId")
    void shouldReturnTokenOnValidLogin() throws Exception {
        UUID anonId = UUID.randomUUID();
        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(authManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        Mockito.when(identityClient.getAnonymousId("alice")).thenReturn(anonId);
        Mockito.when(jwtService.generateToken(Mockito.eq(anonId), Mockito.any()))
                .thenReturn("jwt.fake.token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.fake.token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.anonymousId").value(anonId.toString()));
    }

    @Test
    @DisplayName("POST /login con credenciales inválidas → 401")
    void shouldReturn401OnBadCredentials() throws Exception {
        Mockito.when(authManager.authenticate(Mockito.any()))
                .thenThrow(new BadCredentialsException("bad creds"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("POST /visitor/handoff con anonymousId válido → 200 + payload de handoff")
    void shouldGenerateVisitorHandoffPayload() throws Exception {
        UUID anonId = UUID.randomUUID();
        Mockito.when(jwtService.generateToken(Mockito.eq(anonId), Mockito.any()))
                .thenReturn("visitor.jwt");

        mockMvc.perform(post("/api/v1/auth/visitor/handoff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"anonymousId\":\"" + anonId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("visitor.jwt"))
                .andExpect(jsonPath("$.handoffPayload",
                        org.hamcrest.Matchers.startsWith("HANDOFF_TOKEN:" + anonId)));
    }

    @Test
    @DisplayName("POST /visitor/handoff sin anonymousId → 400")
    void shouldReturn400WhenAnonymousIdMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/visitor/handoff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /login sin Content-Type JSON → 415 Unsupported Media Type")
    void shouldReject415WhenContentTypeNotJson() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
