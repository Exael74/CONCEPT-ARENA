package com.conceptarena.game.app;

import com.conceptarena.game.domain.Round;
import java.util.List;
import java.util.Optional;

public interface RoundRepository {
    Round save(Round round);
    Optional<Round> findById(String id);
    Optional<Round> findActiveRoundByRoomId(String roomId);
    List<Round> findByRoomId(String roomId);
}
