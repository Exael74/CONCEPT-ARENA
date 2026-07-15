# ADR-003: room-service domain state in Redis, outbox in a dedicated minimal Postgres

## Status
Accepted

## Context
The audit's target structure requires room-service to have **no repository, no JPA** — room/participant
state must live in memory, not a relational database (audit 0.3). Separately, the audit's scalability
section (1.3b) flags in-memory state as a horizontal-scaling blocker: if room-service ran on 2+
instances behind a load balancer, per-process memory wouldn't be shared, and participants routed to
different instances would see inconsistent room state.

Both constraints point to the same solution: an external, shared, low-latency store that isn't a
traditional relational database — Redis.

Separately, reliable event publication (ADR-002) needs a transactional outbox, which conventionally
lives in the same relational database as the domain write for atomicity. Redis and a relational
outbox table are two different resource managers, so true atomicity between them isn't available without
distributed transactions (out of scope for this project's size).

## Decision
- `Room`/`Participant` state lives entirely in Redis via Spring Data Redis `@RedisHash` repositories
  (mirroring the JPA repository pattern already familiar in this codebase), with participants nested on
  the room hash (small counts, no need for a separate structure) and a secondary index Set
  (`rooms:active`) for `findActiveRooms()` queries Redis hashes can't answer directly.
- Redis is configured with `maxmemory-policy noeviction` explicitly (never left at a default, which
  varies by image and is often `allkeys-lru`) since this is authoritative domain state, not a cache.
  A write that hits `maxmemory` under `noeviction` fails loudly (`OOM command not allowed`); room-service
  surfaces this as HTTP 503, not an unhandled 500.
- room-service also gets its **own tiny dedicated Postgres instance holding only the `outbox_event`
  table** (see ADR-002) — no domain tables. The write path orders the two writes as: (1) mutate Redis,
  (2) insert the outbox row. If step 2 fails, the Redis write already succeeded — the room/participant
  state is correct, but the event silently fails to publish (a visibility gap, not a data-corruption
  risk). This ordering is deliberate: the reverse order (publish first, mutate Redis second) would risk
  the rest of the system believing a room exists that was never actually created.

## Alternatives considered
- **Redis Streams as the outbox** (atomic `MULTI`/`XADD` alongside the domain write, consumer-group
  polling relays to RabbitMQ): avoids a 5th Postgres instance in the docker-compose stack, but introduces
  a materially more novel pattern (Lua/stream consumer-group semantics) to solve a problem the polling
  outbox already solves identically everywhere else. Rejected in favor of reusing one well-understood
  pattern across every service rather than inventing a second one for room-service alone.

## Consequences
- room-service satisfies both audit constraints (no JPA for domain state, externalized for horizontal
  scaling) simultaneously.
- The Redis/Postgres write-ordering gap above is an accepted, documented risk, not a silent one.
- The docker-compose stack ends up with 4 Postgres instances (auth, game-engine, concept-bank,
  room-service's outbox-only) plus 2 Redis instances (room-service, api-gateway's rate limiter) plus
  RabbitMQ — a large footprint for a course project, accepted as the cost of correctness over minimizing
  infrastructure count.
