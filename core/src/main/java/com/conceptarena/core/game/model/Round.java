package com.conceptarena.core.game.model;

import com.conceptarena.core.shared.valueobject.EntityId;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Round {
    private final EntityId id;
    private final String roomId;
    private final String conceptQuestion;
    private final String expectedAnswer;
    private final int difficulty;
    private final Duration duration;
    private RoundStatus status;
    private final Map<String, Answer> answers;
    private Instant startedAt;
    private Instant endedAt;

    private Round(EntityId id, String roomId, String conceptQuestion, String expectedAnswer, int difficulty, Duration duration, RoundStatus status, Instant startedAt, Instant endedAt) {
        this.id = id;
        this.roomId = roomId;
        this.conceptQuestion = conceptQuestion;
        this.expectedAnswer = expectedAnswer;
        this.difficulty = difficulty;
        this.duration = duration;
        this.status = status;
        this.answers = new HashMap<>();
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public Round(String roomId, String conceptQuestion, String expectedAnswer, int difficulty, Duration duration) {
        this(EntityId.generate(), roomId, conceptQuestion, expectedAnswer, difficulty, duration, RoundStatus.WAITING, null, null);
    }

    public static Round restore(EntityId id, String roomId, String conceptQuestion, String expectedAnswer, int difficulty, Duration duration, RoundStatus status, Instant startedAt, Instant endedAt) {
        return new Round(id, roomId, conceptQuestion, expectedAnswer, difficulty, duration, status, startedAt, endedAt);
    }

    public void restoreAnswer(String userId, String text, Instant submittedAt, Answer.AnswerResult result) {
        Answer answer = new Answer(userId, text, submittedAt, result);
        this.answers.put(userId, answer);
    }

    public void start() {
        this.status = RoundStatus.ACTIVE;
        this.startedAt = Instant.now();
    }

    public void submitAnswer(String userId, String text) {
        if (status != RoundStatus.ACTIVE) {
            throw new IllegalStateException("Round is not active");
        }
        // Belt-and-suspenders: the status flag is flipped asynchronously by either the
        // scheduled timer or the early-end path, so a submission can race in after the
        // deadline but before that flip. Checking the deadline directly closes that window.
        if (startedAt != null && Instant.now().isAfter(startedAt.plus(duration))) {
            throw new IllegalStateException("Time expired for this round");
        }
        if (answers.containsKey(userId)) {
            throw new IllegalStateException("User already answered");
        }
        answers.put(userId, new Answer(userId, text));
    }

    public void end() {
        this.status = RoundStatus.ENDED;
        this.endedAt = Instant.now();
    }

    public boolean allAnswered(List<String> participantIds) {
        return participantIds.stream().allMatch(answers::containsKey);
    }

    public Duration getElapsedTime() {
        if (startedAt == null) return Duration.ZERO;
        Instant end = endedAt != null ? endedAt : Instant.now();
        return Duration.between(startedAt, end);
    }

    public EntityId getId() { return id; }
    public String getRoomId() { return roomId; }
    public String getConceptQuestion() { return conceptQuestion; }
    public String getExpectedAnswer() { return expectedAnswer; }
    public int getDifficulty() { return difficulty; }
    public Duration getDuration() { return duration; }
    public RoundStatus getStatus() { return status; }
    public Map<String, Answer> getAnswers() { return Collections.unmodifiableMap(answers); }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
}
