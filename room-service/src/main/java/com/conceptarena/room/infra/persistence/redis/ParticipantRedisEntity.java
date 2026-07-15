package com.conceptarena.room.infra.persistence.redis;

public class ParticipantRedisEntity {
    private String userId;
    private long joinedAtEpochMilli;
    private boolean microphoneEnabled;

    public ParticipantRedisEntity() {}

    public ParticipantRedisEntity(String userId, long joinedAtEpochMilli, boolean microphoneEnabled) {
        this.userId = userId;
        this.joinedAtEpochMilli = joinedAtEpochMilli;
        this.microphoneEnabled = microphoneEnabled;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public long getJoinedAtEpochMilli() { return joinedAtEpochMilli; }
    public void setJoinedAtEpochMilli(long joinedAtEpochMilli) { this.joinedAtEpochMilli = joinedAtEpochMilli; }
    public boolean isMicrophoneEnabled() { return microphoneEnabled; }
    public void setMicrophoneEnabled(boolean microphoneEnabled) { this.microphoneEnabled = microphoneEnabled; }
}
