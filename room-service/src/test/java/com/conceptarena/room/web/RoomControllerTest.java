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
    @MockBean private MeterRegistry meterRegistry;

    @Test
    void listActiveRoomsReturnsOk() throws Exception {
        Room room = Room.create("Study Room", RoomType.PUBLIC, null, "bank-1", 4);
        when(roomQueryService.getActiveRooms()).thenReturn(List.of(room));

        mockMvc.perform(get("/api/rooms"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("Study Room"));
    }

    @Test
    void getRoomReturnsDetailWithoutInviteCode() throws Exception {
        Room room = Room.create("Private Room", RoomType.PRIVATE, "SECRET", "bank-1", 4);
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

    @Test
    void createRoomReturnsCreated() throws Exception {
        when(commandBus.dispatch(any())).thenReturn("room-123");

        mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RoomController.CreateRoomRequest("Study Room", RoomType.PUBLIC, "bank-1", 4, "user-1"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data").value("room-123"));
    }

    @Test
    void createRoomReturnsBadRequestOnInvalidName() throws Exception {
        when(commandBus.dispatch(any())).thenThrow(new IllegalArgumentException("Room name must not be empty"));

        mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new RoomController.CreateRoomRequest(" ", RoomType.PUBLIC, "bank-1", 4, "user-1"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void joinRoomReturnsOk() throws Exception {
        mockMvc.perform(post("/api/rooms/{id}/join", "room-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RoomController.JoinRoomRequest("user-1"))))
            .andExpect(status().isOk());
    }
}
