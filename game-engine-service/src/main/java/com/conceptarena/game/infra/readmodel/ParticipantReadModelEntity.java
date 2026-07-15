package com.conceptarena.game.infra.readmodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Composite-unique (roomId, userId) so a redelivered RoomJoined upserts instead of throwing a
 * constraint violation that would wedge the RabbitMQ consumer in a requeue loop — see the
 * "idempotency" risk note in ADR-004 / the migration plan's risk callouts.
 */
@Entity
@Table(name = "participant_read_model", uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}))
public class ParticipantReadModelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "room_id", nullable = false)
    private String roomId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
