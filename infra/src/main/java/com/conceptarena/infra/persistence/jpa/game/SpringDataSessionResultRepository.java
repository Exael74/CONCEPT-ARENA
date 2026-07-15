package com.conceptarena.infra.persistence.jpa.game;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpringDataSessionResultRepository extends JpaRepository<SessionResultEntity, String> {
    List<SessionResultEntity> findByUserId(String userId);
    List<SessionResultEntity> findByRoomId(String roomId);
    boolean existsByRoomIdAndUserId(String roomId, String userId);
}
