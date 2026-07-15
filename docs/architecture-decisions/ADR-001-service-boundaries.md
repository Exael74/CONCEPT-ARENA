# ADR-001: Service boundaries and shared kernel scope

## Status
Accepted

## Context
The project originally shipped as a monolito modular (5 Maven modules: core/app/infra/web/bootstrap,
one deployable JVM, an in-process synchronous event bus). An architecture audit required 6 independent
Spring Boot microservices (auth, room, game-engine, concept-bank, voice-signaling) plus an api-gateway,
each with its own database where applicable, communicating over RabbitMQ with a transactional outbox.

## Decision
Split along the domain boundaries already present in the monolith's package structure:
- **auth-service** — user registration/login, JWT issuance.
- **room-service** — room lifecycle, participant membership. State in Redis, not JPA (see ADR-003).
- **game-engine-service** — rounds, answers, scoring, the game saga, and the "session results" reporting
  slice (kept co-located, see ADR-004 for why it isn't split further).
- **concept-bank-service** — concept bank CRUD.
- **voice-signaling-service** — WebRTC signaling relay. No persistence.
- **api-gateway** — routing, gateway-level JWT pre-check, gateway-level rate limiting.

A new `conceptarena-kernel` module holds only the truly generic DDD building blocks with zero Spring
dependencies: `Command`, `DomainEvent`, `EntityId`, `PasswordHash`, `PasswordVerifier`, `DomainException`.
`Email` and the user-specific exceptions (`DuplicateEmailException`, `InvalidCredentialsException`) were
**not** kept in the kernel — they are semantically owned by auth-service even though `Email`'s shape looks
generic, because nothing outside the user/auth domain constructs or validates one.

A separate `jwt-validation-lib` module holds shared *technical* infrastructure (JWT validation, MDC
scoping) reused by room-service, game-engine-service, voice-signaling-service, and api-gateway. This is
deliberately a different module from the kernel, and is not a violation of the audit's "no shared
domain code between services" rule — that rule targets domain DTOs/entities crossing a service boundary
in-process, not reusable technical plumbing like "verify this token's signature."

## Consequences
- Each service can be built, tested, and deployed independently once its extraction phase is complete.
- Cross-domain data needs (game-engine needing room/concept-bank data) are resolved via local read-models
  fed by RabbitMQ events, not synchronous calls or shared repositories — see ADR-004.
- `jwt-validation-lib` centralizes a security-critical algorithm (signature verification) in one place
  instead of 4 independent copies that could drift.
