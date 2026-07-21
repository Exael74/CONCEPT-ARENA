package com.conceptarena.auth.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.conceptarena.auth.app.bus.CommandBus;
import com.conceptarena.auth.domain.error.DuplicateEmailException;
import com.conceptarena.auth.domain.error.InvalidCredentialsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * addFilters = false: pure controller-layer slice test, not a security test — see
 * ConceptBankControllerTest for the same pattern/rationale.
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CommandBus commandBus;
    @MockBean private MeterRegistry meterRegistry;
    // RateLimitingFilter (a Filter bean in the @WebMvcTest slice) now depends on AuthRateLimiter
    // (audit gap #2); provide it so the context loads. addFilters=false means the filter never runs here.
    @MockBean private com.conceptarena.auth.infra.security.AuthRateLimiter rateLimiter;

    @Test
    void registerReturnsCreatedWithUserId() throws Exception {
        when(commandBus.dispatch(any())).thenReturn("user-123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new UserController.RegisterUserRequest("student@escuelaing.edu.co", "password123"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value("user-123"));
    }

    @Test
    void registerReturnsConflictWhenEmailAlreadyExists() throws Exception {
        when(commandBus.dispatch(any())).thenThrow(new DuplicateEmailException("student@escuelaing.edu.co"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new UserController.RegisterUserRequest("student@escuelaing.edu.co", "password123"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void loginReturnsTokenOnSuccess() throws Exception {
        when(commandBus.dispatch(any())).thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new UserController.LoginUserRequest("student@escuelaing.edu.co", "password123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("jwt-token"));
    }

    @Test
    void loginReturnsUnauthorizedOnInvalidCredentials() throws Exception {
        when(commandBus.dispatch(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new UserController.LoginUserRequest("student@escuelaing.edu.co", "wrong"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginReturnsBadRequestForBlankPasswordInsteadOfServerError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new UserController.LoginUserRequest("student@escuelaing.edu.co", " "))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }
}
