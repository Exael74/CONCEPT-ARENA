# ADR-009: room-service uses JPA only for the transactional outbox

## Status
Accepted

## Context
ADR-003 established that room-service keeps its authoritative domain state (Room/Participant) in Redis,
not a relational database — "room-service has no JPA" is the shorthand. But `room-service/pom.xml` does
declare `spring-boot-starter-data-jpa`, and there is an `OutboxEvent` `@Entity`, a `JpaRepository`, and
Flyway migrations. This looks like a contradiction (gap E2).

## Decision
The JPA dependency in room-service exists **solely** for the transactional outbox, and that is
intentional. The outbox pattern (ADR-002) requires writing the event row in the *same local
transaction* as the state change so publication is atomic with the change. A tiny dedicated Postgres
(`room_outbox`) holds only the `outbox_event` table; Room/Participant remain 100% in Redis. Redis has no
multi-key ACID transaction that spans "update room" + "append event" with the same guarantees, so
using a relational outbox here is the pragmatic, correct choice.

The domain layer (`room/domain`, `room/infra/persistence/redis`) has zero JPA; JPA appears only under
`room/infra/messaging/outbox`. An alternative (Redis Streams as the outbox) was considered and rejected:
it would trade a well-understood, queryable, transactional outbox for bespoke stream-trimming and
at-least-once redelivery logic, for no real benefit at this scale.

## Consequences
- The "no JPA" phrasing in ADR-003 is scoped to the domain, not the outbox — documented here to remove
  the apparent contradiction (E2).
- room-service depends on both Redis (domain) and a minimal Postgres (outbox only).
