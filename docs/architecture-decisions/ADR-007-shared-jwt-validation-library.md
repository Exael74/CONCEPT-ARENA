# ADR-007: A shared technical library for JWT validation and MDC scoping

## Status
Accepted

## Context
Four services (room-service, game-engine-service, voice-signaling-service, api-gateway) need to validate
a Bearer/WS-handshake JWT locally without calling auth-service synchronously. The monolith's
`JwtHandshakeInterceptor`/`TokenService`/`JwtTokenProvider` validation logic (parse, verify HS256
signature, extract subject) is a single, security-relevant algorithm. Separately, every service's local
observability handler needs to set/clear MDC keys per-event without wiping the HTTP request's
`requestId` — the monolith's bug here (`MDC.clear()` in a `finally` block, wiping keys set earlier in the
same thread by `CorrelationIdFilter`) is exactly the kind of thing that's easy to reintroduce if copied
by hand into 5 services independently.

## Decision
Extract both into `jwt-validation-lib` (artifact `conceptarena-jwt-validation`):
- `JwtValidator` — parses/verifies an HS256 token against `JWT_SECRET`, returns the subject userId.
  auth-service's own `JwtTokenProvider` becomes a thin wrapper adding `generateToken` (the one method
  only auth-service needs) on top of this shared validator.
- `WsJwtHandshakeInterceptor` / `StompJwtChannelInterceptor` — generic Spring interceptors delegating to
  `JwtValidator`, reused by every service with a raw WS or STOMP endpoint.
- `MdcScope` — an `AutoCloseable` that removes only the specific MDC keys it set (via
  `MDC.remove(key)` in `close()`), never a blanket `MDC.clear()`. Every service's local
  logging/observability event handler uses this instead of hand-rolling try/finally MDC management.

This was a closer call than the JWT decision (the audit's "no shared code between services" framing is
squarely about domain DTOs/entities, not infrastructure utilities) — the tie-breaker is that both pieces
of logic are security- or correctness-critical and easy to subtly re-break if duplicated by hand 4-5
times. A shared library means one fix instead of five independent, potentially-diverging copies.

## Consequences
- `jwt-validation-lib` becomes a dependency of room-service, game-engine-service, voice-signaling-service,
  concept-bank-service, and api-gateway.
- Any future change to the JWT validation algorithm or the MDC-scoping convention is made once.
- This library holds zero domain concepts (no `Room`, `User`, `Round`, etc.) — it is safe to depend on
  from every service without reintroducing the coupling the split was meant to remove.
