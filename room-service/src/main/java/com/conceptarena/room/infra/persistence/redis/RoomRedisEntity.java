package com.conceptarena.room.infra.persistence.redis;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

/**
 * Room/Participant domain state lives 100% in Redis (no JPA) — see
 * docs/architecture-decisions/ADR-003-room-service-redis-state.md. No TTL is set: this is
 * authoritative state, not a cache, so Redis must never silently evict it (see RedisConfig,
 * which enforces maxmemory-policy=noeviction on this connection).
 */
@RedisHash("room")
public class RoomRedisEntity {

    @Id
    private String id;

    private String name;
    private String type;

    @Indexed
    private String inviteCode;

    private String conceptBankId;
    private int maxParticipants;

    @Indexed
    private String status;

    private List<ParticipantRedisEntity> participants = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public String getConceptBankId() { return conceptBankId; }
    public void setConceptBankId(String conceptBankId) { this.conceptBankId = conceptBankId; }
    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<ParticipantRedisEntity> getParticipants() { return participants; }
    public void setParticipants(List<ParticipantRedisEntity> participants) { this.participants = participants; }
}
