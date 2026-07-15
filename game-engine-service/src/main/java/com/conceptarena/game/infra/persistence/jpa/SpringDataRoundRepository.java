package com.conceptarena.game.infra.persistence.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataRoundRepository extends JpaRepository<RoundEntity, String> {
    Optional<RoundEntity> findByRoomIdAndStatus(String roomId, String status);
    List<RoundEntity> findByRoomId(String roomId);
}
