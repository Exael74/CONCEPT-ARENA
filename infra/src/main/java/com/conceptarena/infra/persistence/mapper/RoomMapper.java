package com.conceptarena.infra.persistence.mapper;

import com.conceptarena.core.room.model.Participant;
import com.conceptarena.core.room.model.Room;
import com.conceptarena.core.room.model.RoomStatus;
import com.conceptarena.core.room.model.RoomType;
import com.conceptarena.infra.persistence.jpa.room.ParticipantEntity;
import com.conceptarena.infra.persistence.jpa.room.RoomEntity;

import com.conceptarena.core.shared.valueobject.EntityId;

public class RoomMapper {

    public static ParticipantEntity toEntity(Participant domain, String roomId) {
        ParticipantEntity entity = new ParticipantEntity();
        entity.setRoomId(roomId);
        entity.setUserId(domain.getUserId());
        entity.setJoinedAt(domain.getJoinedAt());
        entity.setMicrophoneEnabled(domain.isMicrophoneEnabled());
        return entity;
    }

    public static RoomEntity toEntity(Room domain) {
        RoomEntity entity = new RoomEntity();
        entity.setId(domain.getId().value());
        entity.setName(domain.getName());
        entity.setType(domain.getType().name());
        entity.setInviteCode(domain.getInviteCode());
        entity.setConceptBankId(domain.getConceptBankId());
        entity.setMaxParticipants(domain.getMaxParticipants());
        entity.setStatus(domain.getStatus().name());
        
        java.util.List<ParticipantEntity> participantEntities = domain.getParticipants().stream()
            .map(p -> toEntity(p, domain.getId().value()))
            .collect(java.util.stream.Collectors.toList());
        entity.setParticipants(participantEntities);
        
        return entity;
    }

    public static Room toDomain(RoomEntity entity) {
        Room room = Room.restore(
            EntityId.from(entity.getId()),
            entity.getName(),
            RoomType.valueOf(entity.getType()),
            entity.getInviteCode(),
            entity.getConceptBankId(),
            entity.getMaxParticipants(),
            RoomStatus.valueOf(entity.getStatus())
        );
        if (entity.getParticipants() != null) {
            entity.getParticipants().forEach(p -> {
                room.restoreParticipant(p.getUserId(), p.getJoinedAt(), p.isMicrophoneEnabled());
            });
        }
        return room;
    }
}
