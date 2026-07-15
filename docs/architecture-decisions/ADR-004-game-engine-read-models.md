# ADR-004: game-engine-service resolves room/concept-bank coupling via local read-models

## Status
Accepted

## Context
In the monolith, `StartRoundCommandHandler` and `SubmitAnswerCommandHandler` (game domain) directly call
`RoomRepository`/`ConceptBankRepository` and mutate `Room` (`room.startGame(); roomRepository.save(room)`)
to check participant membership, mark a room as game-started, and pick a random concept. This is the
single hardest coupling to cut for a physical split: these handlers run on every round start and every
answer submission — potentially the system's hottest path.

## Decision
game-engine-service maintains **local, eventually-consistent read-models**, populated by consuming
RabbitMQ events, instead of calling room-service/concept-bank-service synchronously:

- `RoomReadModelPort` (`findByRoomId`, `isParticipant`, `markGameStarted`) backed by
  `RoomReadModelEntity`/`ParticipantReadModelEntity` in game-engine-service's own Postgres, populated by
  `RoomReadModelEventConsumer` subscribed to `RoomCreated`/`RoomJoined`/`RoomLeft`. `markGameStarted` is
  now purely local — it does not write back to room-service's `Room.status`.
- `ConceptBankReadModelPort` (`pickRandomConcept(bankId)`) backed by
  `ConceptBankReadModelEntity`/`ConceptReadModelEntity`, populated by `ConceptBankReadModelEventConsumer`
  off an **enriched** `ConceptBankCreated` event that now carries the full concept list (see
  `docs/event-contracts.md`) — concept banks are small and effectively immutable after creation (no
  `UpdateConceptBankCommand` exists), so embedding the full list is a reasonable, low-churn trade.
- Both consumers use upsert/delete-if-exists semantics so at-least-once redelivery doesn't throw a
  unique-constraint violation and wedge the RabbitMQ consumer in a requeue loop. A `RoomJoined` arriving
  before its `RoomCreated` (possible under retry-induced reordering) creates a placeholder room row
  rather than dropping the event.

## Alternative considered and rejected
Synchronous REST call (`RoomServiceClient`) wrapped in a Resilience4j circuit breaker + fallback. Rejected
because there is no safe fallback for "is this user a room participant" — that check is a security gate
(anti-cheat/authorization), not optional enrichment. A stale-by-milliseconds local read-model is strictly
safer than "let the answer through because room-service was briefly unreachable." Circuit breakers are the
right tool when a sane degraded response exists; they are the wrong tool for an authorization check.

## Consequences
- game-engine-service can start rounds and accept answers without room-service or concept-bank-service
  being reachable at that instant, at the cost of eventual (not immediate) consistency for room/bank data.
- `GameSaga` stays intra-process with these handlers (it actively dispatches `StartRoundCommand` via the
  local command bus) — only the repository calls were decoupled, not the saga's process boundary.
- The "session results" reporting slice (`SessionResultEventHandler`/`SessionQueryService`) stays
  co-located inside game-engine-service rather than becoming its own service, since it needs `Round`/
  `Answer` detail that only exists in this service's own database.
