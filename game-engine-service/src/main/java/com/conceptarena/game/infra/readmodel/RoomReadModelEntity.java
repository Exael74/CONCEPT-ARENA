package com.conceptarena.game.infra.readmodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Local, eventually-consistent copy of room-service's Room aggregate, populated by
 * RoomReadModelEventConsumer off RabbitMQ — see ADR-004. conceptBankId is nullable to tolerate
 * a RoomJoined arriving before its RoomCreated (out-of-order redelivery edge case).
 */
@Entity
@Table(name = "room_read_model")
public class RoomReadModelEntity {

    @Id
    private String roomId;

    private String creatorUserId;

    private String conceptBankId;

    @Column(nullable = false)
    private int maxParticipants;

    @Column(nullable = false)
    private boolean gameStarted;

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getCreatorUserId() { return creatorUserId; }
    public void setCreatorUserId(String creatorUserId) { this.creatorUserId = creatorUserId; }
    public String getConceptBankId() { return conceptBankId; }
    public void setConceptBankId(String conceptBankId) { this.conceptBankId = conceptBankId; }
    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }
    public boolean isGameStarted() { return gameStarted; }
    public void setGameStarted(boolean gameStarted) { this.gameStarted = gameStarted; }
}
