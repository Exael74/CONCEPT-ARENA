package com.conceptarena.auth.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.conceptarena.auth.app.UserRepository;
import com.conceptarena.auth.app.bus.CommandBus;
import com.conceptarena.auth.domain.Email;
import com.conceptarena.auth.domain.User;
import com.conceptarena.auth.domain.Username;
import com.conceptarena.auth.domain.error.DuplicateUsernameException;
import com.conceptarena.kernel.valueobject.PasswordHash;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

/** addFilters = false: a pure controller-layer slice test — the authenticated principal is
 *  injected directly via .principal(...), mirroring what JwtBearerAuthenticationFilter would set. */
@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private UserRepository userRepository;
    @MockBean private CommandBus commandBus;
    @MockBean private MeterRegistry meterRegistry;
    // RateLimitingFilter (a Filter bean in the @WebMvcTest slice) depends on AuthRateLimiter; provide
    // it so the context loads. addFilters=false means the filter never actually runs here.
    @MockBean private com.conceptarena.auth.infra.security.AuthRateLimiter rateLimiter;

    private static final UsernamePasswordAuthenticationToken AUTH =
        new UsernamePasswordAuthenticationToken("user-1", null, List.of());

    private User user() {
        return User.register(new Email("student@escuelaing.edu.co"), new Username("student"), PasswordHash.fromHash("hashed"));
    }

    @Test
    void meReturnsProfileForAuthenticatedUser() throws Exception {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user()));

        mockMvc.perform(get("/api/auth/me").principal(AUTH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value("student@escuelaing.edu.co"))
            .andExpect(jsonPath("$.data.username").value("student"))
            .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void meReturns404WhenUserRecordIsMissing() throws Exception {
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/me").principal(AUTH))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateUsernameReturnsUpdatedProfileOnSuccess() throws Exception {
        User user = user();
        user.changeUsername(new Username("newname"));
        when(commandBus.dispatch(any())).thenReturn("newname");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        mockMvc.perform(patch("/api/auth/me/username").principal(AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ProfileController.UpdateUsernameRequest("newname"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value("newname"));
    }

    @Test
    void updateUsernameReturnsConflictWhenTaken() throws Exception {
        when(commandBus.dispatch(any())).thenThrow(new DuplicateUsernameException("taken"));

        mockMvc.perform(patch("/api/auth/me/username").principal(AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ProfileController.UpdateUsernameRequest("taken"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateUsernameReturnsBadRequestOnInvalidFormat() throws Exception {
        mockMvc.perform(patch("/api/auth/me/username").principal(AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ProfileController.UpdateUsernameRequest("a"))))
            .andExpect(status().isBadRequest());
    }
}
