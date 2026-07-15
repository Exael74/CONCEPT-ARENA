# ADR-005: HS256 with a shared secret, not RS256+JWKS

## Status
Accepted

## Context
Once JWTs are validated by 5 independent services (auth-service issues them; room-service,
game-engine-service, voice-signaling-service, and api-gateway each need to validate them locally without
a synchronous call back to auth-service on every request/WS message), the signing scheme choice matters:
symmetric HS256 (one shared secret, anyone holding it can both sign and verify) versus asymmetric RS256
with a JWKS endpoint (auth-service holds the private key; everyone else fetches/caches the public key and
can verify but not forge tokens).

## Decision
Keep HS256 with a shared `JWT_SECRET` environment variable, matching the monolith's existing pattern,
validated locally by every service via `jwt-validation-lib`'s `JwtValidator`.

RS256+JWKS's main security benefit — services that can verify tokens without being able to forge them —
matters most when you don't equally trust every service holding the key, or when third parties outside
your deployment need to verify tokens. Neither applies here: this is a single-developer academic project
where every service is deployed together via one docker-compose file operated by one person. The
operational cost of RS256+JWKS (key generation, rotation, hosting a JWKS endpoint, public/private key
distribution across 5 services) buys a security property this deployment doesn't need.

## Consequences
- Every service needs `JWT_SECRET` set to the identical value (`.env.example` documents this).
- If this system ever adds services operated by a different trust boundary (a third-party integration,
  a partner's service consuming the API), this decision should be revisited — HS256's shared secret means
  any service that can verify tokens can also mint them.
