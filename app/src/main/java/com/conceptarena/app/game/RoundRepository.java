package com.conceptarena.app.game;

import com.conceptarena.core.game.model.Round;
import java.util.Optional;
import java.util.List;

public interface RoundRepository {
    Round save(Round round);
    Optional<Round> findById(String id);
    Optional<Round> findActiveRoundByRoomId(String roomId);
    List<Round> findByRoomId(String roomId);
}
