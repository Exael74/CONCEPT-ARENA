package com.conceptarena.game.infra.persistence.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSessionResultRepository extends JpaRepository<SessionResultEntity, String> {
    List<SessionResultEntity> findByUserId(String userId);
    List<SessionResultEntity> findByRoomId(String roomId);
    boolean existsByRoomIdAndUserId(String roomId, String userId);
}
