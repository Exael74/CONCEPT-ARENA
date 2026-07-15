package com.conceptarena.room.infra.persistence.redis;

import com.conceptarena.kernel.valueobject.EntityId;
import com.conceptarena.room.domain.Room;
import com.conceptarena.room.domain.RoomStatus;
import com.conceptarena.room.domain.RoomType;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class RoomRedisMapper {

    public static RoomRedisEntity toEntity(Room domain) {
        RoomRedisEntity entity = new RoomRedisEntity();
        entity.setId(domain.getId().value());
        entity.setName(domain.getName());
        entity.setType(domain.getType().name());
        entity.setInviteCode(domain.getInviteCode());
        entity.setConceptBankId(domain.getConceptBankId());
        entity.setMaxParticipants(domain.getMaxParticipants());
        entity.setStatus(domain.getStatus().name());
        entity.setParticipants(domain.getParticipants().stream()
            .map(p -> new ParticipantRedisEntity(p.getUserId(), p.getJoinedAt().toEpochMilli(), p.isMicrophoneEnabled()))
            .collect(Collectors.toList()));
        return entity;
    }

    public static Room toDomain(RoomRedisEntity entity) {
        Room room = Room.restore(
            EntityId.from(entity.getId()),
            entity.getName(),
            RoomType.valueOf(entity.getType()),
            entity.getInviteCode(),
            entity.getConceptBankId(),
            entity.getMaxParticipants(),
            RoomStatus.valueOf(entity.getStatus())
        );
        List<ParticipantRedisEntity> participants = entity.getParticipants();
        if (participants != null) {
            participants.forEach(p ->
                room.restoreParticipant(p.getUserId(), Instant.ofEpochMilli(p.getJoinedAtEpochMilli()), p.isMicrophoneEnabled()));
        }
        return room;
    }
}
