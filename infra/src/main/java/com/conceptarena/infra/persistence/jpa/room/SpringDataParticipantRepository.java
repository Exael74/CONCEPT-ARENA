package com.conceptarena.infra.persistence.jpa.room;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpringDataParticipantRepository extends JpaRepository<ParticipantEntity, String> {
    List<ParticipantEntity> findByRoomId(String roomId);
    void deleteByRoomIdAndUserId(String roomId, String userId);
}
