package com.conceptarena.voice.dto;

/**
 * type: "join" | "leave" | "offer" | "answer" | "ice-candidate".
 *
 * Inbound (client -> server): toUserId addresses the relay target; fromUserId is ignored — the
 * handler always uses the handshake-authenticated userId instead (see SignalingWebSocketHandler),
 * the same anti-spoofing fix applied to game-engine-service's GameWebSocketHandler (audit gap #1's
 * class of bug).
 * Outbound (server -> client, relayed): fromUserId identifies who sent it; toUserId is null (the
 * recipient already knows it's addressed to them, since it's on their own socket).
 */
public record SignalingMessage(String type, String roomId, String toUserId, String fromUserId, String payload) {
}
