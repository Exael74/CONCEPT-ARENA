package com.conceptarena.auth.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.conceptarena.auth.app.bus.CommandBus;
import com.conceptarena.auth.domain.error.InvalidOtpException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OtpController.class)
@AutoConfigureMockMvc(addFilters = false)
class OtpControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CommandBus commandBus;
    @MockBean private com.conceptarena.auth.infra.security.AuthRateLimiter rateLimiter;
    @MockBean private MeterRegistry meterRegistry;

    @BeforeEach
    void allowRateLimit() {
        when(rateLimiter.allow(any())).thenReturn(true);
    }

    @Test
    void requestReturnsGeneric200RegardlessOfWhetherEmailExists() throws Exception {
        mockMvc.perform(post("/api/auth/otp/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OtpController.RequestOtpRequest("student@escuelaing.edu.co"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void requestReturns400OnInvalidEmailWithoutDispatching() throws Exception {
        mockMvc.perform(post("/api/auth/otp/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OtpController.RequestOtpRequest("not-an-email"))))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(commandBus);
    }

    @Test
    void verifyReturnsTokenOnSuccess() throws Exception {
        when(commandBus.dispatch(any())).thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new OtpController.VerifyOtpRequest("student@escuelaing.edu.co", "123456"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("jwt-token"));
    }

    @Test
    void verifyReturns401OnInvalidCode() throws Exception {
        when(commandBus.dispatch(any())).thenThrow(new InvalidOtpException("Invalid or expired code"));

        mockMvc.perform(post("/api/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new OtpController.VerifyOtpRequest("student@escuelaing.edu.co", "000000"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void verifyReturns400OnBlankCodeWithoutDispatching() throws Exception {
        mockMvc.perform(post("/api/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new OtpController.VerifyOtpRequest("student@escuelaing.edu.co", "  "))))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(commandBus);
    }
}
