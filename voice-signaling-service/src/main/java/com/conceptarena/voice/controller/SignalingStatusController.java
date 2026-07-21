package com.conceptarena.voice.controller;

import com.conceptarena.voice.dto.ApiResponse;
import com.conceptarena.voice.websocket.SignalingPresenceRegistry;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/signaling")
public class SignalingStatusController {

    private final SignalingPresenceRegistry presenceRegistry;

    public SignalingStatusController(SignalingPresenceRegistry presenceRegistry) {
        this.presenceRegistry = presenceRegistry;
    }

    @GetMapping("/{roomId}/participants")
    public ResponseEntity<ApiResponse<Set<String>>> participants(@PathVariable String roomId) {
        return ResponseEntity.ok(ApiResponse.success(
            "Connected participants for room: " + roomId, presenceRegistry.participantsOf(roomId)));
    }
}
