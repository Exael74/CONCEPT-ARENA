# ADR-011: WebSocket authentication carries the JWT in the handshake query string

## Status
Accepted (with mitigations)

## Context
Browsers cannot set an `Authorization` header on a WebSocket upgrade request (the WS API exposes no way
to add headers to the handshake). So the JWT travels as `?token=` on the handshake URL, validated by
`WsJwtHandshakeInterceptor`. Tokens in URLs are a known weakness (gap A2): they can leak via proxy access
logs, browser history, and `Referer` headers.

## Decision
Keep handshake-URL token transport as the primary mechanism, because it is the only option that works
from a browser without a custom sub-protocol, and harden it:

- **Short TTL** — WS-capable tokens expire in 2h (compose override), limiting the window of a leaked one.
- **No URL logging** — nginx `access_log off` for `/ws/` paths; the handshake URL is never written to a
  log we control.
- **TLS/WSS in production** — under WSS the URL (including the token) is encrypted on the wire.
- **Defense-in-depth (future work)** — validate a token presented in the STOMP `CONNECT` frame header via
  a `ChannelInterceptor` in `configureClientInboundChannel`, so a deployment can move the token off the
  URL entirely once the frontend is updated to send it in `CONNECT` instead of `?token=`.

Moving the token to the `CONNECT` frame *today* was rejected: it would break the current frontend
contract (which sends `?token=`), and the frontend is deployed and versioned separately (out of this
repo's scope), so the two can't change atomically here.

## Consequences
- The residual risk is explicitly accepted and bounded by the mitigations above (resolves A2 as a
  documented decision, not an open gap).
- The `CONNECT`-frame path is noted as the migration target when the frontend can be updated in lockstep.
