package com.conceptarena.room.domain;

import java.time.Instant;

public class Participant {
    private final String userId;
    private final Instant joinedAt;
    private boolean microphoneEnabled;

    public Participant(String userId) {
        this.userId = userId;
        this.joinedAt = Instant.now();
        this.microphoneEnabled = false;
    }

    public Participant(String userId, Instant joinedAt, boolean microphoneEnabled) {
        this.userId = userId;
        this.joinedAt = joinedAt;
        this.microphoneEnabled = microphoneEnabled;
    }

    public String getUserId() { return userId; }
    public Instant getJoinedAt() { return joinedAt; }
    public boolean isMicrophoneEnabled() { return microphoneEnabled; }
    public void toggleMicrophone() { this.microphoneEnabled = !this.microphoneEnabled; }
}
