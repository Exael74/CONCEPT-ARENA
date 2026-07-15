package com.conceptarena.game.infra.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface JpaParticipantReadModelRepository extends JpaRepository<ParticipantReadModelEntity, String> {
    boolean existsByRoomIdAndUserId(String roomId, String userId);

    @Transactional
    void deleteByRoomIdAndUserId(String roomId, String userId);
}
