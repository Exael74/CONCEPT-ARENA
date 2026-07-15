# ADR-006: STOMP broker relay via RabbitMQ, not Redis pub/sub

## Status
Accepted

## Context
The monolith's STOMP configuration uses Spring's `SimpleBroker` — an in-process, single-instance message
broker. Audit gap #6: if a service with STOMP subscribers ran on multiple instances behind a load
balancer, a message published from the instance handling the publish-side request would never reach
STOMP clients connected to a different instance of the same service. Two standard fixes exist: relay
STOMP frames through an external broker (`enableStompBrokerRelay`, e.g. against RabbitMQ's STOMP plugin),
or fan out via Redis pub/sub and have every instance forward to its own locally-connected clients.

## Decision
Use `enableStompBrokerRelay` against RabbitMQ's STOMP plugin (`rabbitmq_stomp`), not Redis pub/sub.
RabbitMQ is already being stood up for the outbox/event-bus wiring (ADR-002); enabling its STOMP plugin
reuses infrastructure already present instead of adding a second pub/sub system solely for WebSocket
fan-out. `StompRoomNotificationAdapter` (room-service) and `StompGameNotificationAdapter`
(game-engine-service) each get their own broker-relay configuration.

## Consequences
- Correct-by-construction for room-service, which does run with `deploy.replicas: 2` in the
  docker-compose demonstration.
- Applied identically to game-engine-service for consistency and future-readiness, even though — per the
  GameSaga scaling decision (see the Phase 9 ADR on GameSaga's single-replica limitation) —
  game-engine-service is not actually run at more than one replica today; the broker relay fix alone
  doesn't make its in-memory saga state safe across instances.
- STOMP endpoints are split per-service (`/ws/room-stomp`, `/ws/game-stomp` instead of one shared `/ws`),
  a small breaking change to the WS contract that any connecting frontend must adopt.
