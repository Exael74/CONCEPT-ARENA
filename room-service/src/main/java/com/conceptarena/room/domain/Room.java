package com.conceptarena.room.domain;

import com.conceptarena.kernel.valueobject.EntityId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Room {
    private static final int MAX_NAME_LENGTH = 100;

    private final EntityId id;
    private final String name;
    private final RoomType type;
    private final String inviteCode;
    private final String creatorUserId;
    private final String conceptBankId;
    private final int maxParticipants;
    private RoomStatus status;
    private final List<Participant> participants;

    private Room(EntityId id, String name, RoomType type, String inviteCode, String creatorUserId,
                 String conceptBankId, int maxParticipants, RoomStatus status) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.inviteCode = inviteCode;
        this.creatorUserId = creatorUserId;
        this.conceptBankId = conceptBankId;
        this.maxParticipants = maxParticipants;
        this.status = status;
        this.participants = new ArrayList<>();
    }

    public static Room create(String name, RoomType type, String inviteCode, String creatorUserId,
                               String conceptBankId, int maxParticipants) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Room name must not be empty");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Room name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        if (maxParticipants <= 0) {
            throw new IllegalArgumentException("maxParticipants must be greater than 0");
        }
        return new Room(EntityId.generate(), name.trim(), type, inviteCode, creatorUserId, conceptBankId, maxParticipants, RoomStatus.WAITING);
    }

    public static Room restore(EntityId id, String name, RoomType type, String inviteCode, String creatorUserId,
                                String conceptBankId, int maxParticipants, RoomStatus status) {
        return new Room(id, name, type, inviteCode, creatorUserId, conceptBankId, maxParticipants, status);
    }

    public void restoreParticipant(String userId, java.time.Instant joinedAt, boolean microphoneEnabled) {
        Participant participant = new Participant(userId, joinedAt, microphoneEnabled);
        participants.add(participant);
    }

    public Participant addParticipant(String userId) {
        if (participants.size() >= maxParticipants) {
            throw new IllegalStateException("Room is full");
        }
        if (participants.stream().anyMatch(p -> p.getUserId().equals(userId))) {
            throw new IllegalStateException("User already in room");
        }
        Participant participant = new Participant(userId);
        participants.add(participant);
        return participant;
    }

    public boolean removeParticipant(String userId) {
        return participants.removeIf(p -> p.getUserId().equals(userId));
    }

    public boolean isEmpty() {
        return participants.isEmpty();
    }

    public void startGame() {
        this.status = RoomStatus.IN_GAME;
    }

    public void finish() {
        this.status = RoomStatus.FINISHED;
    }

    public EntityId getId() { return id; }
    public String getName() { return name; }
    public RoomType getType() { return type; }
    public String getInviteCode() { return inviteCode; }
    public String getCreatorUserId() { return creatorUserId; }
    public String getConceptBankId() { return conceptBankId; }
    public int getMaxParticipants() { return maxParticipants; }
    public RoomStatus getStatus() { return status; }
    public List<Participant> getParticipants() { return Collections.unmodifiableList(participants); }
    public int getParticipantCount() { return participants.size(); }
    public Optional<Participant> findParticipant(String userId) {
        return participants.stream().filter(p -> p.getUserId().equals(userId)).findFirst();
    }
}
