# ADR-002: Transactional outbox via scheduled polling, not CDC

## Status
Accepted

## Context
Each service that owns a database (auth, room [outbox-only, see ADR-003], game-engine, concept-bank)
needs to publish domain events to RabbitMQ reliably: an event must be published if and only if the
transaction that produced it committed. Writing to the database and publishing to the broker are two
different resource managers, so a naive "save, then publish" is not atomic — a crash between the two
either loses the event or publishes one for a transaction that got rolled back.

Two standard solutions exist: **Debezium/CDC** (tail the database's write-ahead log, publish changes as
they land) and a **transactional outbox with a polling publisher** (write the event to an `outbox_event`
table in the same transaction as the domain write; a separate process polls unpublished rows and
publishes them).

## Decision
Use the polling publisher. `OutboxEvent` (id, aggregateId, eventType, exchange, routingKey, payload JSON,
createdAt, publishedAt, attempts, lastError) is written in the same `@Transactional` method as the domain
write. A `@Scheduled(fixedDelay = 500)` `OutboxEventPublisher` reads unpublished rows
(`SELECT ... WHERE published_at IS NULL ORDER BY created_at LIMIT 100 FOR UPDATE SKIP LOCKED`), publishes
each via `RabbitTemplate` with publisher confirms enabled, and marks `publishedAt` on ack.

Debezium was rejected: it requires running Kafka Connect (or Debezium Server) and enabling logical
replication on Postgres — real operational surface that isn't justified for a single-developer academic
project targeting docker-compose, not a Kubernetes cluster with a dedicated ops story. The polling
publisher gives the identical at-least-once delivery guarantee using infrastructure the services already
need (their own Postgres, RabbitMQ).

`shedlock-spring` + `shedlock-provider-jdbc-template` guard the poller against double-publishing if a
service ever runs with more than one replica.

## Consequences
- Publish latency is bounded by the poll interval (≤500ms), not instantaneous — acceptable for this
  system's event types (none are on a hard real-time path; the WS timer/round-state fan-out stays
  in-process per service, not routed through the outbox).
- Consumers must be idempotent (see ADR-004's read-model upsert requirement) since delivery is
  at-least-once, not exactly-once.
- `OutboxEvent.payload` is a JSON snapshot; every service's outbox/consumer `ObjectMapper` is configured
  with `FAIL_ON_UNKNOWN_PROPERTIES = false` so future payload shape changes don't break old rows or
  older/newer consumers reading the same exchange.
- No cleanup job for published rows exists yet; unbounded growth of `outbox_event` is accepted as a
  known, explicitly out-of-scope gap for this project's size.
