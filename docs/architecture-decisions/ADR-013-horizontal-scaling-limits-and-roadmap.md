# ADR-013: Horizontal-scaling limits and roadmap

## Status
Accepted

## Context
Several components hold per-instance state that constrains running >1 replica. This ADR records the
current position for each and the concrete path to remove the limit, so they are tracked decisions
rather than silent gaps (S3, S5, S11, S12, D7).

## Decisions

### S5 — Round timers are per-JVM (`ScheduledTimerAdapter`)
The round-timeout `ScheduledFuture` lives in a JVM-local map, so a timer set on replica A is invisible
to replica B. **Mitigation already in place:** the *effect* of a timer firing (ending the round) is
guarded by `RoundEndGuard`, which has a Redis implementation (`SETNX`) that makes round-end
idempotent and single-fire across replicas — so a duplicated/early end can't double-apply. **Roadmap:**
externalize the schedule itself by storing each round's deadline in Redis (a sorted set keyed by
expiry) and having every replica run a short sweep that claims and ends expired rounds via
`RoundEndGuard`. Until then, game-engine round *timeouts* assume a single active timer owner, while
early-end (all-answered) already works multi-replica.

### S3 — No database read replicas
The four Postgres instances are single-node. **Position:** at this scale (a class project / demo) a
read replica is premature; the read-heavy path (session-results/ranking) is now fronted by an
in-process cache (S4). **Roadmap:** `docs/overlays/` shows the streaming-replication compose overlay;
wiring it in needs a read-only `DataSource` + `@Transactional(readOnly=true)` routing in
game-engine's query service. Deferred until real read load justifies it.

### S11 — Lobby raw-WS sessions are per-JVM (`LobbyWebSocketHandler`)
The raw `/ws/lobby` handler keeps its `WebSocketSession` set in a static field and its `broadcast()`
has **no callers** — the actual lobby fan-out goes over STOMP (`StompRoomNotificationAdapter` →
`/topic/lobby`), which under the docker profile uses the shared RabbitMQ STOMP relay (S6), so it
already reaches clients on every replica. The raw handler is a thin echo endpoint; no distributed
state is required. Resolved by the STOMP relay, documented here.

### S12 — Voice signaling presence is per-JVM (`SignalingPresenceRegistry`)
A WebRTC signaling exchange is inherently tied to the single instance each peer's socket is connected
to. Sharing presence across replicas needs a pub/sub relay (Redis Pub/Sub), not just a shared
key-value store, because signaling is a live message relay, not stored state. **Position:** voice is
best-effort and runs single-instance today; this is an accepted limit. **Roadmap:** a Redis Pub/Sub
fan-out of signaling messages keyed by roomId. Deferred (out of core gameplay path).

### D7 — `jwt-validation-lib` stays a separate module (not merged into the kernel)
Keeping token validation + WS interceptors in their own library (rather than folding them into
`conceptarena-kernel`) is deliberate and already justified in ADR-007: the kernel is Spring-free DDD
primitives, whereas jwt-validation-lib is Spring-Security/Servlet/WS glue. Merging would drag a
security/servlet footprint into the pure-domain kernel and into services (voice, gateway) that use the
JWT lib but intentionally don't depend on the kernel (ADR-010). No change; recorded here to close D7.

## Consequences
- S11 and D7 are resolved (STOMP relay / existing ADR-007) — no code owed.
- S5, S3, S12 have explicit, bounded mitigations and a documented roadmap rather than being open,
  undocumented risks.
