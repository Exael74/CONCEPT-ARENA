package com.conceptarena.game.infra.persistence.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataRoundRepository extends JpaRepository<RoundEntity, String> {
    // A plain findByRoomIdAndStatus(...): Optional<RoundEntity> throws IncorrectResultSizeDataAccessException
    // if more than one row matches — which could happen transiently before @Version locking was added
    // (see RoundEntity#version) and for any round already double-active on an already-running DB.
    // Ordered + "first" makes this defensive: always returns the most recently started match instead
    // of ever crashing on a duplicate.
    List<RoundEntity> findByRoomIdAndStatusOrderByStartedAtDesc(String roomId, String status);
    List<RoundEntity> findByRoomId(String roomId);
}
