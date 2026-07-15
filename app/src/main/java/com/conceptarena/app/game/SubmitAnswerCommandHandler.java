package com.conceptarena.app.game;

import com.conceptarena.app.bus.CommandHandler;
import com.conceptarena.app.bus.EventBus;
import com.conceptarena.app.room.RoomRepository;
import com.conceptarena.core.game.command.SubmitAnswerCommand;
import com.conceptarena.core.game.event.AnswerRejected;
import com.conceptarena.core.game.event.AnswerSubmitted;
import com.conceptarena.core.game.model.Answer;
import com.conceptarena.core.game.model.Round;
import com.conceptarena.core.room.model.Room;
import org.springframework.stereotype.Service;

@Service
public class SubmitAnswerCommandHandler implements CommandHandler<SubmitAnswerCommand, Void> {

    private static final int MAX_ANSWER_LENGTH = 500;

    private final EventBus eventBus;
    private final RoundRepository roundRepository;
    private final RoomRepository roomRepository;
    private final AnswerValidationPort answerValidationPort;

    public SubmitAnswerCommandHandler(EventBus eventBus, RoundRepository roundRepository,
                                       RoomRepository roomRepository, AnswerValidationPort answerValidationPort) {
        this.eventBus = eventBus;
        this.roundRepository = roundRepository;
        this.roomRepository = roomRepository;
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

        Room room = roomRepository.findById(command.roomId()).orElse(null);
        if (room == null || room.findParticipant(command.userId()).isEmpty()) {
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

        roundRepository.save(round);

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

