# Event Contracts

Reverse-documented from the actual code (audit gap #6 remediation, 2026-07-15) — every table
below matches `OutboxWritingEventHandler` in the corresponding service exactly. If this file and
the code ever disagree, the code is right and this file is stale; update it in the same PR that
changes an event's shape or routing.

All events are published via the transactional outbox pattern (see
[ADR-002](architecture-decisions/ADR-002-outbox-pattern-scheduled-polling.md)):
`OutboxEventPublisher` polls every 500ms and calls
`rabbitTemplate.convertAndSend(exchange, routingKey, payload)`. As of this remediation pass, the
`payload` also carries a `correlationId` message header propagated from the original HTTP
request's `X-Request-Id` (see `CorrelationIdFilter` in each service) — see the "Correlation"
section below.

Every exchange is a **durable topic exchange**, declared in code by
`infra/messaging/config/RabbitTopologyConfig` in each service (audit gap #2 remediation — no
service declared any RabbitMQ topology before this pass; queues would never have existed on a
real broker).

## auth-service — exchange `conceptarena.auth.events`

Source: `auth-service/src/main/java/com/conceptarena/auth/infra/messaging/outbox/OutboxWritingEventHandler.java`.
No cross-service consumer exists today for any of these events.

| eventType | routing key | aggregateId | payload (JSON) |
|---|---|---|---|
| `UserRegistered` | `auth.user-registered` | userId | `{eventId, occurredOn, aggregateId, email: {value}}` |
| `UserLoggedIn` | `auth.user-logged-in` | userId | `{eventId, occurredOn, aggregateId}` |
| `UserVerified` | `auth.user-verified` | userId | `{eventId, occurredOn, aggregateId}` |

`UserVerified` is published when a registrant completes OTP email verification and their account
transitions from inactive to active (see `VerifyOtpCommandHandler`) — distinct from `UserLoggedIn`,
which fires on every successful authentication (password or OTP), including the one that
immediately follows verification.

## room-service — exchange `conceptarena.room.events`

Source: `room-service/src/main/java/com/conceptarena/room/infra/messaging/outbox/OutboxWritingEventHandler.java`.
Consumed by `game-engine-service`'s `RoomReadModelEventConsumer` (see below) to build a local
read-model used by `SubmitAnswerCommandHandler`'s participant check — see
[ADR-004](architecture-decisions/ADR-004-game-engine-read-models.md).

| eventType | routing key | aggregateId | payload (JSON) |
|---|---|---|---|
| `RoomCreated` | `room.room-created` | roomId | `{roomId, name, type, creatorUserId, conceptBankId, maxParticipants}` — **note**: this is a custom `RoomCreatedPayload` record, not the raw domain event; `inviteCode` is deliberately excluded (stays private to room-service) |
| `RoomJoined` | `room.room-joined` | roomId | `{eventId, occurredOn, aggregateId, userId}` — whole-event Jackson serialization; `aggregateId` is the roomId |
| `RoomLeft` | `room.room-left` | roomId | `{eventId, occurredOn, aggregateId, userId}` — same shape as `RoomJoined` |

## concept-bank-service — exchange `conceptarena.conceptbank.events`

Source: `concept-bank-service/src/main/java/com/conceptarena/conceptbank/infra/messaging/outbox/OutboxWritingEventHandler.java`.
Consumed by `game-engine-service`'s `ConceptBankReadModelEventConsumer`.

| eventType | routing key | aggregateId | payload (JSON) |
|---|---|---|---|
| `ConceptBankCreated` | `conceptbank.concept-bank-created` | bankId | `{eventId, occurredOn, aggregateId, name, subject, concepts: [{question, expectedAnswer, difficulty}, ...]}` — carries the **full concept list** (not just name/subject) so game-engine never needs a synchronous call back to this service; safe because banks have no update command today |

## game-engine-service — exchange `conceptarena.game.events`

Source: `game-engine-service/src/main/java/com/conceptarena/game/infra/messaging/outbox/OutboxWritingEventHandler.java`.
No cross-service consumer exists today. `AnswerRejected` is deliberately **not** published here —
it stays local-only (same-process `MicrometerMetricsAdapter` only).

| eventType | routing key | aggregateId | payload (JSON) |
|---|---|---|---|
| `RoundStarted` | `game.round-started` | roundId | `{eventId, occurredOn, aggregateId, roomId, conceptQuestion, difficulty, durationSeconds}` |
| `AnswerSubmitted` | `game.answer-submitted` | roundId | `{eventId, occurredOn, aggregateId, roomId, userId, answerText, expectedAnswer}` |
| `RoundEnded` | `game.round-ended` | roundId | `{eventId, occurredOn, aggregateId, roomId, scores: {userId: points}, results: {userId: "CORRECT"\|"INCORRECT"}}` |
| `GameEnded` | `game.game-ended` | roomId | `{eventId, occurredOn, aggregateId, roomId, finalScores: {userId: points}}` |

## Cross-service consumers (queue bindings)

Declared in `game-engine-service/src/main/java/com/conceptarena/game/infra/messaging/config/RabbitTopologyConfig.java`:

| queue | bound to exchange | routing key | handler |
|---|---|---|---|
| `game-engine.room.created.readmodel` | `conceptarena.room.events` | `room.room-created` | `RoomReadModelEventConsumer.onRoomCreatedMessage` |
| `game-engine.room.joined.readmodel` | `conceptarena.room.events` | `room.room-joined` | `RoomReadModelEventConsumer.onRoomJoinedMessage` |
| `game-engine.room.left.readmodel` | `conceptarena.room.events` | `room.room-left` | `RoomReadModelEventConsumer.onRoomLeftMessage` |
| `game-engine.conceptbank.created.readmodel` | `conceptarena.conceptbank.events` | `conceptbank.concept-bank-created` | `ConceptBankReadModelEventConsumer.onConceptBankCreatedMessage` |

All four consumers are idempotent (upsert / exists-check before insert) to tolerate RabbitMQ's
at-least-once redelivery — see each consumer's class Javadoc.

## Correlation

Every `OutboxEvent` row carries a `correlationId` column (added by
`db/migration/V*__add_correlation_id_to_outbox_event.sql` in each service), populated from
`MDC.get("requestId")` at the point the row is written (same value `CorrelationIdFilter` put in
the MDC for the original HTTP request). `OutboxEventPublisher` sets it as a RabbitMQ message
header (`correlationId`) on publish; every `@RabbitListener` reads that header back into its own
MDC before processing, so a single `X-Request-Id` is traceable end-to-end across the HTTP →
outbox → RabbitMQ → consumer hop, not just within one service's own request lifecycle.

## Known gaps (do not mark ✅ until fixed)

- No dead-letter queue / retry-limit policy is configured for any of the four read-model queues —
  a consumer exception currently requeues forever (`SimpleRabbitListenerContainerFactory`
  default), which can wedge a queue under a persistent bad message.
- No schema registry or contract test enforces that this file and the code stay in sync; it is
  hand-maintained. Treat any PR that changes an event's fields or routing key as required to
  update this file in the same diff.
