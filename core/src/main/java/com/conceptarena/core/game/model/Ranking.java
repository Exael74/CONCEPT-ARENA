package com.conceptarena.core.game.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Ranking {
    private final String roomId;
    private final List<PlayerScore> scores;

    public Ranking(String roomId) {
        this.roomId = roomId;
        this.scores = new ArrayList<>();
    }

    public void addScore(PlayerScore score) {
        scores.add(score);
        Collections.sort(scores);
    }

    public List<PlayerScore> getScores() {
        return Collections.unmodifiableList(scores);
    }

    public String getRoomId() { return roomId; }
}
