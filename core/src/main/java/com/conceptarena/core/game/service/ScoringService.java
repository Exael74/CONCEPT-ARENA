package com.conceptarena.core.game.service;

import com.conceptarena.core.game.model.Answer;
import com.conceptarena.core.game.model.Round;
import java.time.Duration;

public class ScoringService {

    public static int calculateScore(Round round, Answer answer) {
        if (answer.getResult() != Answer.AnswerResult.CORRECT) {
            return 0;
        }
        int puntosBase = round.getDifficulty() * 10;
        
        if (round.getStartedAt() == null || answer.getSubmittedAt() == null) {
            return puntosBase;
        }

        double durationTotalSec = round.getDuration().getSeconds();
        if (durationTotalSec <= 0) {
            return puntosBase;
        }

        double elapsedSec = (double) Duration.between(round.getStartedAt(), answer.getSubmittedAt()).toMillis() / 1000.0;
        double tiempoRestanteSec = Math.max(0.0, durationTotalSec - elapsedSec);
        
        double ratio = tiempoRestanteSec / durationTotalSec;
        double bonusRapidez = Math.max(0.0, ratio * puntosBase * 0.5);
        
        return (int) Math.round(puntosBase + bonusRapidez);
    }
}
