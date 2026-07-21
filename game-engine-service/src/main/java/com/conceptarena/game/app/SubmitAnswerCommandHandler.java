package com.conceptarena.game.app;

import com.conceptarena.game.app.bus.CommandHandler;
import com.conceptarena.game.app.bus.EventBus;
import com.conceptarena.game.app.readmodel.RoomReadModelPort;
import com.conceptarena.game.domain.Answer;
import com.conceptarena.game.domain.Round;
import com.conceptarena.game.domain.command.SubmitAnswerCommand;
import com.conceptarena.game.domain.event.AnswerRejected;
import com.conceptarena.game.domain.event.AnswerSubmitted;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * Rewritten against RoomReadModelPort.isParticipant instead of the monolith's
 * RoomRepository.findById(...).findParticipant(...) — see ADR-004. This is a security-relevant
 * check (anti-cheat/authorization), so the read-model consumer's idempotent upsert semantics
 * matter here: a stale-by-milliseconds local view is acceptable, an unavailable room-service is
 * not (the whole point of decoupling this check from a synchronous call).
 */
@Service
public class SubmitAnswerCommandHandler implements CommandHandler<SubmitAnswerCommand, Void> {

    private static final int MAX_ANSWER_LENGTH = 500;

    private final EventBus eventBus;
    private final RoundRepository roundRepository;
    private final RoomReadModelPort roomReadModelPort;
    private final AnswerValidationPort answerValidationPort;

    public SubmitAnswerCommandHandler(EventBus eventBus, RoundRepository roundRepository,
                                       RoomReadModelPort roomReadModelPort, AnswerValidationPort answerValidationPort) {
        this.eventBus = eventBus;
        this.roundRepository = roundRepository;
        this.roomReadModelPort = roomReadModelPort;
        this.answerValidationPort = answerValidationPort;
    }

    @Override
    public Void handle(SubmitAnswerCommand command) {
        if (command.answerText() == null || command.answerText().isBlank()) {
            reject(command, "blank_answer");
            throw new IllegalArgumentException("Answer text must not be empty");
        }
        if (command.answerText().length() > MAX_ANSWER_LENGTH) {
            reject(command, "answer_too_long");
            throw new IllegalArgumentException("Answer text exceeds maximum length of " + MAX_ANSWER_LENGTH);
        }

        if (!roomReadModelPort.isParticipant(command.roomId(), command.userId())) {
            reject(command, "not_a_participant");
            throw new IllegalStateException("User " + command.userId() + " is not a participant of room " + command.roomId());
        }

        Round round = roundRepository.findActiveRoundByRoomId(command.roomId()).orElse(null);
        if (round == null) {
            reject(command, "no_active_round");
            throw new IllegalStateException("No active round for room: " + command.roomId());
        }

        try {
            round.submitAnswer(command.userId(), command.answerText());
        } catch (IllegalStateException e) {
            reject(command, e.getMessage() != null && e.getMessage().contains("expired") ? "round_expired" : "duplicate_answer");
            throw e;
        }

        Answer answer = round.getAnswers().get(command.userId());
        boolean correct = answerValidationPort.isCorrect(command.answerText(), round.getExpectedAnswer());
        if (correct) {
            answer.markCorrect();
        } else {
            answer.markIncorrect();
        }

        try {
            roundRepository.save(round);
        } catch (DataIntegrityViolationException duplicate) {
            // Two requests from the same user raced past the per-instance check on separate Round
            // copies; the answers(round_id, user_id) primary key rejected the second insert. Surface
            // it as a clean duplicate rejection (400), not an unhandled 500 (audit gap #1).
            reject(command, "duplicate_answer");
            throw new IllegalStateException("User already answered", duplicate);
        } catch (ObjectOptimisticLockingFailureException staleRound) {
            // This Round was loaded, then concurrently ended/transitioned (early-end or timer
            // expiry) by the time this save ran — the @Version bump means this save would otherwise
            // silently resurrect an ended round back to ACTIVE (found in production 2026-07-21: left
            // 2 rounds simultaneously ACTIVE for one room, crashing every subsequent lookup with
            // IncorrectResultSizeDataAccessException — 500s on /answer, and could match an answer
            // against the WRONG round's expectedAnswer). Surface as a clean rejection (400): the
            // round moved on, so this submission no longer applies.
            reject(command, "round_transitioned");
            throw new IllegalStateException("Round ended before this answer was saved — try the current round", staleRound);
        }

        eventBus.publish(new AnswerSubmitted(
            round.getId().value(), command.roomId(), command.userId(),
            command.answerText(), round.getExpectedAnswer()
        ));

        return null;
    }

    private void reject(SubmitAnswerCommand command, String reason) {
        eventBus.publish(new AnswerRejected(command.roomId(), command.userId(), reason));
    }
}
