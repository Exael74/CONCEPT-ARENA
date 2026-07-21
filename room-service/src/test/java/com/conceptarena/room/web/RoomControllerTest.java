package com.conceptarena.room.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.conceptarena.room.app.RoomQueryService;
import com.conceptarena.room.app.RoomRepository;
import com.conceptarena.room.app.bus.CommandBus;
import com.conceptarena.room.domain.Room;
import com.conceptarena.room.domain.RoomType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RoomController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoomControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CommandBus commandBus;
    @MockBean private RoomQueryService roomQueryService;
    @MockBean private RoomRepository roomRepository;
    @MockBean private RoomActionRateLimiter rateLimiter;
    @MockBean private MeterRegistry meterRegistry;

    @org.junit.jupiter.api.BeforeEach
    void allowRateLimitByDefault() {
        // Write endpoints now gate on the rate limiter (audit gap #5); default it to "allow" so the
        // existing happy-path tests exercise the endpoint logic, not the limiter.
        when(rateLimiter.allow(any())).thenReturn(true);
    }

    @Test
    void listActiveRoomsReturnsOk() throws Exception {
        Room room = Room.create("Study Room", RoomType.PUBLIC, null, "creator-1", "bank-1", 4);
        when(roomQueryService.getActiveRooms()).thenReturn(List.of(room));

        mockMvc.perform(get("/api/rooms"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("Study Room"));
    }

    @Test
    void getRoomReturnsDetailWithoutInviteCode() throws Exception {
        Room room = Room.create("Private Room", RoomType.PRIVATE, "SECRET", "creator-1", "bank-1", 4);
        room.addParticipant("user-1");
        when(roomRepository.findById(room.getId().value())).thenReturn(Optional.of(room));

        mockMvc.perform(get("/api/rooms/{id}", room.getId().value()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Private Room"))
            .andExpect(jsonPath("$.data.participantCount").value(1))
            .andExpect(jsonPath("$.data.inviteCode").doesNotExist());
    }

    @Test
    void getRoomReturnsNotFoundForUnknownId() throws Exception {
        when(roomRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rooms/{id}", "missing"))
            .andExpect(status().isNotFound());
    }

    private static Principal principal(String userId) {
        return () -> userId;
    }

    @Test
    void createRoomReturnsCreatedWithRoomIdAndInviteCode() throws Exception {
        Room room = Room.create("Study Room", RoomType.PRIVATE, "ABC123", "creator-1", "bank-1", 4);
        when(commandBus.dispatch(any())).thenReturn(room.getId().value());
        when(roomRepository.findById(room.getId().value())).thenReturn(Optional.of(room));

        mockMvc.perform(post("/api/rooms")
                .principal(principal("user-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RoomController.CreateRoomRequest("Study Room", RoomType.PRIVATE, "bank-1", 4))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.roomId").value(room.getId().value()))
            .andExpect(jsonPath("$.data.inviteCode").value("ABC123"));
    }

    @Test
    void createRoomReturnsBadRequestOnInvalidName() throws Exception {
        when(commandBus.dispatch(any())).thenThrow(new IllegalArgumentException("Room name must not be empty"));

        mockMvc.perform(post("/api/rooms")
                .principal(principal("user-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RoomController.CreateRoomRequest(" ", RoomType.PUBLIC, "bank-1", 4))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void joinRoomReturnsOk() throws Exception {
        mockMvc.perform(post("/api/rooms/{id}/join", "room-1")
                .principal(principal("user-1")))
            .andExpect(status().isOk());
    }

    /**
     * Audit gap #5: room write endpoints must enforce a per-user rate limit (room-service had
     * none). Must return 429 and never reach the command bus when the limiter rejects.
     */
    @Test
    void joinRoomReturnsTooManyRequestsWhenRateLimitExceeded() throws Exception {
        when(rateLimiter.allow("spammer")).thenReturn(false);

        mockMvc.perform(post("/api/rooms/{id}/join", "room-1")
                .principal(principal("spammer")))
            .andExpect(status().isTooManyRequests());
        org.mockito.Mockito.verifyNoInteractions(commandBus);
    }

    @Test
    void joinRoomUsesAuthenticatedPrincipalNotAnyClientSuppliedValue() throws Exception {
        mockMvc.perform(post("/api/rooms/{id}/join", "room-1")
                .principal(principal("real-user")))
            .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<com.conceptarena.room.domain.command.JoinRoomCommand> captor =
            org.mockito.ArgumentCaptor.forClass(com.conceptarena.room.domain.command.JoinRoomCommand.class);
        org.mockito.Mockito.verify(commandBus).dispatch(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().userId()).isEqualTo("real-user");
    }
}
