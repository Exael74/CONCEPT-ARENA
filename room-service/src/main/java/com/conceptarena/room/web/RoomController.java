package com.conceptarena.room.web;

import com.conceptarena.room.app.RoomQueryService;
import com.conceptarena.room.app.RoomRepository;
import com.conceptarena.room.app.bus.CommandBus;
import com.conceptarena.room.domain.Room;
import com.conceptarena.room.domain.RoomType;
import com.conceptarena.room.domain.command.CreateRoomCommand;
import com.conceptarena.room.domain.command.JoinRoomCommand;
import com.conceptarena.room.domain.command.LeaveRoomCommand;
import com.conceptarena.room.web.dto.ApiResponse;
import java.security.Principal;
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
    private final RoomActionRateLimiter rateLimiter;

    public RoomController(CommandBus commandBus, RoomQueryService roomQueryService,
                          RoomRepository roomRepository, RoomActionRateLimiter rateLimiter) {
        this.commandBus = commandBus;
        this.roomQueryService = roomQueryService;
        this.roomRepository = roomRepository;
        this.rateLimiter = rateLimiter;
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
        // HashMap (not Map.of): creatorUserId is null for rooms created before this field existed
        // (no backfill possible for Redis-held state) — Map.of throws NPE on a null value.
        Map<String, Object> detail = new java.util.HashMap<>();
        detail.put("id", room.getId().value());
        detail.put("name", room.getName());
        detail.put("type", room.getType().name());
        detail.put("status", room.getStatus().name());
        detail.put("creatorUserId", room.getCreatorUserId());
        detail.put("conceptBankId", room.getConceptBankId());
        detail.put("maxParticipants", room.getMaxParticipants());
        detail.put("participantCount", room.getParticipantCount());
        detail.put("participants", participants);
        return detail;
    }

    /**
     * Audit gap #1's class of bug, found by extension while validating game-engine-service's fix
     * (2026-07-15 remediation): userId used to come straight from the request body on every
     * write endpoint below, unverified against the caller's own JWT — a user authenticated as A
     * could create/join/leave rooms as B. Now always taken from the authenticated principal
     * (same fix as GameController.submitAnswer); the request bodies no longer carry a userId.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createRoom(@RequestBody CreateRoomRequest request, Principal principal) {
        if (!rateLimiter.allow(principal.getName())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error("Too many room actions, slow down"));
        }
        try {
            var command = new CreateRoomCommand(
                request.name(), request.type(), request.conceptBankId(),
                request.maxParticipants(), principal.getName()
            );
            String roomId = commandBus.dispatch(command);
            // The invite code is returned once, only to the creator (the caller of this
            // endpoint) — GET /api/rooms/{id} keeps omitting it so non-creators can't bypass
            // the "join by code" gate of private rooms.
            String inviteCode = roomRepository.findById(roomId)
                .map(Room::getInviteCode)
                .orElse(null);
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("roomId", roomId);
            body.put("inviteCode", inviteCode);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room created", body));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<String>> joinRoom(@PathVariable String id, Principal principal) {
        if (!rateLimiter.allow(principal.getName())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error("Too many room actions, slow down"));
        }
        try {
            String roomId = commandBus.dispatch(new JoinRoomCommand(id, principal.getName(), null));
            return ResponseEntity.ok(ApiResponse.success("Joined room", roomId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /** Returns the joined roomId — the caller only knows the code, not the room. */
    @PostMapping("/join/{code}")
    public ResponseEntity<ApiResponse<String>> joinRoomByCode(@PathVariable String code, Principal principal) {
        if (!rateLimiter.allow(principal.getName())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error("Too many room actions, slow down"));
        }
        try {
            String roomId = commandBus.dispatch(new JoinRoomCommand(null, principal.getName(), code));
            return ResponseEntity.ok(ApiResponse.success("Joined room by code", roomId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(@PathVariable String id, Principal principal) {
        if (!rateLimiter.allow(principal.getName())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error("Too many room actions, slow down"));
        }
        try {
            commandBus.dispatch(new LeaveRoomCommand(id, principal.getName()));
            return ResponseEntity.ok(ApiResponse.success("Left room", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    public record CreateRoomRequest(String name, RoomType type, String conceptBankId, int maxParticipants) {}
}
