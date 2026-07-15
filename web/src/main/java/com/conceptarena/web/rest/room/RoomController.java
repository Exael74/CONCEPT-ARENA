package com.conceptarena.web.rest.room;

import com.conceptarena.app.bus.CommandBus;
import com.conceptarena.app.room.RoomQueryService;
import com.conceptarena.app.room.RoomRepository;
import com.conceptarena.core.room.command.CreateRoomCommand;
import com.conceptarena.core.room.command.JoinRoomCommand;
import com.conceptarena.core.room.command.LeaveRoomCommand;
import com.conceptarena.core.room.model.Room;
import com.conceptarena.core.room.model.RoomType;
import com.conceptarena.web.rest.dto.ApiResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final CommandBus commandBus;
    private final RoomQueryService roomQueryService;
    private final RoomRepository roomRepository;

    public RoomController(CommandBus commandBus, RoomQueryService roomQueryService, RoomRepository roomRepository) {
        this.commandBus = commandBus;
        this.roomQueryService = roomQueryService;
        this.roomRepository = roomRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listActiveRooms() {
        List<Map<String, Object>> rooms = roomQueryService.getActiveRooms().stream()
            .map(r -> Map.<String, Object>of(
                "id", r.getId().value(),
                "name", r.getName(),
                "type", r.getType().name(),
                "status", r.getStatus().name(),
                "participants", r.getParticipantCount(),
                "maxParticipants", r.getMaxParticipants()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Active rooms", rooms));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoom(@PathVariable String id) {
        return roomRepository.findById(id)
            .map(room -> ResponseEntity.ok(ApiResponse.success("Room detail", toDetail(room))))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Room not found: " + id)));
    }

    private Map<String, Object> toDetail(Room room) {
        List<Map<String, Object>> participants = room.getParticipants().stream()
            .map(p -> Map.<String, Object>of(
                "userId", p.getUserId(),
                "joinedAt", p.getJoinedAt().toString(),
                "microphoneEnabled", p.isMicrophoneEnabled()
            ))
            .toList();
        // inviteCode is intentionally omitted: exposing it here would let any
        // authenticated user bypass the "join by code" gate for private rooms.
        return Map.of(
            "id", room.getId().value(),
            "name", room.getName(),
            "type", room.getType().name(),
            "status", room.getStatus().name(),
            "conceptBankId", room.getConceptBankId(),
            "maxParticipants", room.getMaxParticipants(),
            "participantCount", room.getParticipantCount(),
            "participants", participants
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createRoom(@RequestBody CreateRoomRequest request) {
        try {
            var command = new CreateRoomCommand(
                request.name(), request.type(), request.conceptBankId(),
                request.maxParticipants(), request.userId()
            );
            String roomId = commandBus.dispatch(command);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room created", roomId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<Void>> joinRoom(@PathVariable String id,
                                                       @RequestBody JoinRoomRequest request) {
        try {
            commandBus.dispatch(new JoinRoomCommand(id, request.userId(), null));
            return ResponseEntity.ok(ApiResponse.success("Joined room", null));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/join/{code}")
    public ResponseEntity<ApiResponse<Void>> joinRoomByCode(@PathVariable String code,
                                                              @RequestBody JoinRoomRequest request) {
        try {
            commandBus.dispatch(new JoinRoomCommand(null, request.userId(), code));
            return ResponseEntity.ok(ApiResponse.success("Joined room by code", null));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(@PathVariable String id,
                                                        @RequestBody LeaveRoomRequest request) {
        try {
            commandBus.dispatch(new LeaveRoomCommand(id, request.userId()));
            return ResponseEntity.ok(ApiResponse.success("Left room", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    public record CreateRoomRequest(String name, RoomType type, String conceptBankId,
                                    int maxParticipants, String userId) {}
    public record JoinRoomRequest(String userId) {}
    public record LeaveRoomRequest(String userId) {}
}

