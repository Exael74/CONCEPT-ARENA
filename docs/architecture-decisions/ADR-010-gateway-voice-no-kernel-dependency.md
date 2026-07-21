# ADR-010: api-gateway and voice-signaling-service do not depend on the DDD kernel

## Status
Accepted

## Context
The four domain services (auth, room, game-engine, concept-bank) depend on `conceptarena-kernel` for the
DDD primitives (EntityId, DomainEvent, Command, DomainException, value objects). `api-gateway` and
`voice-signaling-service` do not (gaps E3, E4). This breaks surface-level uniformity across the modules.

## Decision
Keep it that way — the absence is correct, not an oversight:

- **api-gateway** is a reactive Spring Cloud Gateway. It has no domain model, no aggregates, no domain
  events; it routes HTTP/WS and applies cross-cutting concerns (CORS, security headers, correlation id,
  rate limiting). Pulling in the kernel would add an unused dependency and, worse, a servlet-oriented
  transitive footprint to a WebFlux app. The gateway's correlation-id concern is met by a reactive
  `GlobalFilter` (`CorrelationIdGlobalFilter`), not the servlet `CorrelationIdFilter`.
- **voice-signaling-service** is a stateless WebRTC signaling relay: it exchanges SDP offers/answers and
  ICE candidates between currently-connected peers. It has no persistent domain model either, so it uses
  a flatter `websocket/dto/controller` structure and needs no kernel primitives.

Forcing a kernel dependency on these two purely for uniformity would be cargo-cult consistency.

## Consequences
- Two modules intentionally omit the kernel; this ADR records why so it isn't flagged as drift again
  (resolves E3's kernel point and E4).
- Both still share `jwt-validation-lib` for token validation, which is the dependency they actually need.
