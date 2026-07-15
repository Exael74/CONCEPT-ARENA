package com.conceptarena.infra.persistence.jpa.game;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SpringDataRoundRepository extends JpaRepository<RoundEntity, String> {
    Optional<RoundEntity> findByRoomIdAndStatus(String roomId, String status);
    List<RoundEntity> findByRoomId(String roomId);
}
