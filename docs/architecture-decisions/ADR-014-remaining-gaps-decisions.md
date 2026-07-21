# ADR-014: Decisions on the remaining audit gaps (gaps-restantes.md)

## Status
Accepted

## Context
The `gaps-restantes.md` audit lists items where the right resolution is a documented engineering
decision (already mitigated, deliberately deferred, or a deliberate design choice) rather than more
code. This ADR records those so they are tracked decisions, not silent omissions. Items resolved with
actual code (O3, O5, O6, S2, S7, D2, D5, A4, A5, C4–C7, C1 schemas, deploy files) are not listed here.

## Decisions

### S6 — No circuit breaker (resilience4j) is added
The system has **no synchronous inter-service calls** — services communicate only through events
(ADR-004). The failure modes a circuit breaker guards (a slow/failing downstream dependency) are
already handled by the design: the transactional **outbox + scheduled poller** retries failed
publishes, **publisher-confirms** (A7-config) detect a broker that didn't accept a message, the
**DLQ** (A1) drains poison messages instead of looping, and the RabbitListener **receive-timeout**
(S7) bounds a stuck consumer. Database access is local per service. Adding resilience4j here would be
ceremony with no cross-service call to protect. Revisit only if a synchronous call is ever introduced.

### S3 — Round timers remain per-JVM (with the double-fire already prevented)
`ScheduledTimerAdapter` schedules the round-end on the replica that started the round. The dangerous
part of this — two replicas both ending the same round — is **already prevented** by
`RedisRoundEndGuard` (SETNX claim), so round-end is single-fire and idempotent across replicas. The
residual risk is narrow: if the *owning* replica dies mid-round, that round's timeout won't fire on
another replica (early-end still works if all remaining players answer). Fully closing it needs a
Redis-backed deadline registry + a per-replica sweep that claims expired deadlines via the existing
guard. Deferred (roadmap in ADR-013); the blast radius is one stalled round, not data loss.

### S1 — No PostgreSQL read replicas
Single-node per DB. The read-heavy path (session-results / ranking) is now fronted by the S4 Caffeine
cache, which absorbs repeated ranking reads before they hit Postgres. A streaming-replication overlay
plus an `AbstractRoutingDataSource` splitting reads/writes is the next step, deferred until real read
load justifies the added operational complexity (roadmap in ADR-013).

### S5 — In-memory rate limiter stays the default; Redis is the docker profile
`matchIfMissing = true` (in-memory) is deliberate so unit tests need no Redis. Every deployed profile
(`docker`, and the k8s manifests) sets `app.rate-limiter.store=redis`, so the shared-store limiter is
what actually runs in any multi-replica environment. The only configuration where the in-memory
default applies is a single local instance, where per-JVM counting is correct. No change.

### S8 — STOMP relay enabled only under the docker profile
Same rationale as S5: `enableSimpleBroker` is the default so tests need no broker; every deployed
profile sets `app.stomp.relay.enabled=true` (RabbitMQ STOMP relay), which is what fans out across
replicas. Accepted as-is.

### A2 — Voice membership check is local presence, not an authoritative room-service query
`SignalingWebSocketHandler` now requires the sender to have `join`ed the room's signaling channel
before it can relay into it (local presence check — closes the "relay into a room you never joined"
hole for the common case). An authoritative cross-check against room-service membership is deferred:
voice is a best-effort enhancement (not the game), and the local check already blocks the practical
abuse. Roadmap: consult a room read-model or a lightweight room-service call on `join`.

### A6 — JWT key rotation / revocation is Post-MVP
Single HS256 secret today. Multi-key rotation (kid header) + a Redis token-revocation blacklist is a
sizeable security feature deferred to Post-MVP. Mitigations in place: short TTL (2h), and the secret is
injected (never committed) via `JWT_SECRET`/k8s Secret. Rotating the secret invalidates all tokens,
which is the break-glass path until per-key rotation exists.

### E1 — `messaging/outbox/` is kept (not renamed to `messaging/publisher/`)
The `outbox/` package holds the whole outbox mechanism — `OutboxEvent` (entity), its repository, the
`OutboxWritingEventHandler`, AND `OutboxEventPublisher`. Renaming it to `publisher/` would be *less*
accurate (it's not only a publisher), and would churn imports across four services for a cosmetic
change. The audited "publisher/ + config/" structure is satisfied in spirit: `config/` exists
(RabbitTopologyConfig), and `outbox/` is the correctly-named home of the pattern. Kept deliberately.

## Verification notes (O1, O2 — verified, not re-implemented)
- **O1 (tracing)** was verified end-to-end against the running stack: Zipkin reports spans from all six
  services, and the trace context propagates over HTTP and across the RabbitMQ hop
  (`observation-enabled` on template + listener). The Zipkin datasource is now provisioned in Grafana (O5).
- **O2 (Grafana dashboard)** was verified against the running stack: Prometheus scrapes all targets UP,
  and the provisioned "ConceptArena — Overview" dashboard loads with its Prometheus/Loki panels. See
  `scripts/load/README.md` for the load run that populated it.
