# Frontend Integration Guide

Everything the frontend needs to talk to the ConceptArena backend cleanly: base URLs, the auth flow
(including the just-added username field and account-verification semantics), every REST endpoint,
every WebSocket/STOMP channel, and the conventions (response envelope, error codes, rate limits) that
apply across all of them.

**Single entry point:** the frontend only ever talks to the **api-gateway**. It never calls
auth-service/room-service/game-engine-service/concept-bank-service/voice-signaling-service directly —
the gateway routes by path (see `api-gateway/src/main/resources/application*.yml`).

## 1. Environment

| | Value |
|---|---|
| Production API base URL | `http://104.46.113.22:8080` |
| CORS | Open (`allowedOriginPatterns: *`, `allowCredentials: true`) — any origin can call it, no allowlist needed |
| Local dev base URL | `http://localhost:8080` (`docker compose up`) |

There is no HTTPS/TLS on the production IP right now (plain HTTP on :8080). Only port 8080 is
publicly reachable on the VM; everything else is bound to `127.0.0.1`.

## 2. Response envelope (every REST endpoint)

```json
{ "success": true, "message": "human-readable text", "data": <T | null> }
```

`success: false` on errors; `data` is usually `null` then and `message` has the reason. Always read
`data`, never assume the raw body — nothing returns bare JSON.

**HTTP status conventions used everywhere:** `400` invalid input, `401` bad/missing/expired
credentials or JWT, `403` blocked by a rule (e.g. rate limit's a `429`, not 403 — see below), `404`
not found, `409` conflict (duplicate email/username), `429` rate-limited, `500` unexpected.

## 3. Auth flow (auth-service, routed at `/api/auth/**`)

**This just changed: registration now requires a username, and OTP is account verification, not an
alternative login.** A freshly registered account is **inactive** and cannot log in with a password
until it's verified via the emailed code.

```
1. POST /api/auth/register        { email, username, password } -> 201, data: userId
     -> account created INACTIVE, verification email sent automatically
2. POST /api/auth/otp/verify      { email, code } -> 200, data: JWT
     -> activates the account AND logs it in (returns a usable token immediately)
3. (only if the code expired/was lost) POST /api/auth/otp/request  { email } -> 200, resends a code
4. POST /api/auth/login           { email, password } -> 200, data: JWT   (only works once verified)
```

### `POST /api/auth/register`
Request: `{ "email": "student@escuelaing.edu.co", "username": "student1", "password": "..." }`
- `username`: **3–20 chars, letters/numbers/underscore only** (`^[A-Za-z0-9_]{3,20}$`), must be unique.
- Response `data`: the new `userId` (string). Account is inactive — do not treat this as "logged in".
- `409` if the email OR username is already taken (message says which).
- `400` on invalid email/username format.

### `POST /api/auth/otp/verify`
Request: `{ "email": "...", "code": "123456" }` (code is the 6-digit string from the email).
Response `data`: a JWT — use it immediately (`Authorization: Bearer <token>`), no separate login call
needed. `401` on wrong/expired/already-used/too-many-attempts code (message is generic on purpose —
doesn't distinguish the reason, to avoid leaking account state).

### `POST /api/auth/otp/request`
Request: `{ "email": "..." }`. **Always returns 200 with the same generic message** whether the email
is registered, unregistered, or already verified — this is intentional anti-enumeration, not a bug.
Don't use the response to infer account state.

### `POST /api/auth/login`
Request: `{ "email": "...", "password": "..." }`. Response `data`: a JWT. `401` on wrong password OR
on an unverified/inactive account (same generic message either way — don't tell the user "verify your
email" from this response; if login fails right after registration, prompt them to check their inbox
and use the verify screen instead).

### Profile — `/api/auth/me` (requires `Authorization: Bearer <JWT>`, unlike the endpoints above)

- `GET /api/auth/me` -> `data: { id, email, username, active, registeredAt }`
- `PATCH /api/auth/me/username` — body `{ "username": "newname" }` -> `data`: same profile shape with
  the new username. `409` if taken, `400` if it fails the format rule. This is how the user edits
  their username later from a profile/settings screen.

**Email OTP template branding** (in case the frontend renders anything resend-related): primary
`#3B82F6`, secondary `#7C3AED`, tertiary `#10B981`, neutral `#0F172A`.

## 4. JWT usage

- REST: `Authorization: Bearer <token>` header.
- WebSocket handshake: browsers can't set a header on a WS upgrade, so pass it as a query param
  instead: `?token=<jwt>` on every `/ws/**` URL below. A missing/invalid token rejects the handshake.
- The JWT subject is the `userId` — that's what `principal.getName()` / `authentication.getName()`
  returns server-side, and what to decode client-side if you need the userId without a round-trip
  (e.g. `JSON.parse(atob(token.split('.')[1])).sub`).

## 5. Rooms (room-service, `/api/rooms/**`)

| Method | Path | Auth | Body | Notes |
|---|---|---|---|---|
| GET | `/api/rooms` | public | — | lobby list: id, name, type, status, participants, maxParticipants |
| GET | `/api/rooms/{id}` | public | — | detail incl. participants; **inviteCode never included** |
| POST | `/api/rooms` | JWT | `{ name, type, conceptBankId, maxParticipants }` | `type`: `PUBLIC` \| `PRIVATE`; response `data: { roomId, inviteCode }` — invite code is returned **once**, only to the creator |
| POST | `/api/rooms/{id}/join` | JWT | — | join a public room by id |
| POST | `/api/rooms/join/{code}` | JWT | — | join a private room by invite code; response `data`: the roomId |
| POST | `/api/rooms/{id}/leave` | JWT | — | |

`userId` is **never** taken from the request body on any of these — always the JWT. Room-action
endpoints are rate-limited per user; a `429` means "slow down", not a real error.

## 6. Concept banks (concept-bank-service, `/api/concept-banks/**`)

| Method | Path | Auth | Body |
|---|---|---|---|
| POST | `/api/concept-banks` | none currently required | `{ name, subject, concepts: [{ question, expectedAnswer, difficulty }] }` |
| GET | `/api/concept-banks` | public | list: id, name, subject, conceptCount |
| GET | `/api/concept-banks/{id}` | public | detail — **`expectedAnswer` is never returned**, only `question`/`difficulty`, so don't expect it while building a "review the bank" screen |

Answer checking is lowercase + accent-insensitive server-side (`café` = `cafe` = `CAFÉ`) — don't
duplicate that normalization client-side, just send what the user typed.

## 7. Game (game-engine-service, `/api/game/**`, `/api/sessions/**`)

| Method | Path | Auth | Body |
|---|---|---|---|
| POST | `/api/game/{roomId}/start` | JWT | — starts the round loop for a room |
| POST | `/api/game/{roomId}/answer` | JWT | `{ answerText }` — rate-limited to 3/sec/user, same limiter as the WS path below |
| GET | `/api/game/{roomId}/ranking` | public | `data`: `{ userId: score, ... }` |
| GET | `/api/sessions/results?userId={id}` | public | a user's historical session results |
| GET | `/api/sessions/results/room/{roomId}` | public | one room's results |

## 8. Voice signaling (voice-signaling-service, `/api/signaling/**`, `/ws/signaling`)

`GET /api/signaling/{roomId}/participants` -> `data`: set of connected userIds. **Note:** this REST
route was just added to the gateway (it existed on the service but had no gateway route before) — it
needs a redeploy to be live in production; ask before relying on it if testing against
`104.46.113.22` immediately.

WebRTC signaling itself goes over the raw WS endpoint (`/ws/signaling`, see §9) with JSON messages:
```
{ "type": "join" | "leave" | "offer" | "answer" | "ice-candidate",
  "roomId": "...", "toUserId": "...", "payload": "..." }
```
Outbound (server -> client) messages have `fromUserId` set instead of `toUserId` (you already know
it's addressed to you — it's on your own socket). `toUserId`/`fromUserId` you send are informational
only for routing; the server always trusts the handshake-authenticated userId as the real sender, so
you can't spoof `fromUserId`.

## 9. WebSocket / STOMP channels

Two flavors exist. **STOMP is the one to use for lobby/room/round updates** (raw endpoints are
lower-level/legacy paths kept for the same services).

| Endpoint | Protocol | Purpose |
|---|---|---|
| `/ws/room-stomp` | STOMP (+SockJS fallback) | room/lobby events |
| `/ws/game-stomp` | STOMP | round/game events |
| `/ws/lobby` | raw WS | legacy lobby broadcast |
| `/ws/game` | raw WS | legacy gameplay channel |
| `/ws/signaling` | raw WS | WebRTC signaling (see §8) |

All require `?token=<jwt>` on the connect URL. STOMP topics to `SUBSCRIBE` to (server -> client only,
you never `SEND` to these — they're broadcast):

| Topic | Payload `type` values | Fields |
|---|---|---|
| `/topic/lobby` | `ROOM_CREATED` | `roomId, name, roomType` |
| `/topic/rooms/{roomId}/participants` | `USER_JOINED`, `USER_LEFT` | `userId` |
| `/topic/rooms/{roomId}/round` | `ROUND_STARTED` | `roundId, question, difficulty, durationSeconds` |
| | `ROUND_ENDED` | `roundId, scores` |
| | `GAME_ENDED` | `finalScores` |
| `/topic/rooms/{roomId}/timer` | (timer ticks — game-engine `ScheduledTimerAdapter`) | |

Subscribe to `/topic/rooms/{roomId}/participants` and `/topic/rooms/{roomId}/round` right after
joining a room to drive the waiting-room and in-game UI reactively instead of polling the REST GETs.

## 10. Rate limits (so 429s aren't a surprise)

- Auth (`/api/auth/{register,login,otp/request,otp/verify}`): shared bucket, 10 req/min per IP.
- Room actions (create/join/leave): per-user limiter.
- Game answers (REST and WS): 3/sec per user, same limiter on both transports — can't bypass by
  switching transport.
- WS connection attempts (voice signaling): rate-limited per IP at handshake.

On `429`, back off and retry — don't treat it as a fatal error in the UI; show "slow down" messaging.

## 11. Known caveats to design around

- **No HTTPS on the current deployment.** If the frontend origin is HTTPS (e.g. Azure Container Apps
  default), calling `http://104.46.113.22:8080` from it may hit mixed-content blocking in browsers —
  confirm the deployed frontend's own reverse proxy (not direct browser calls) is what talks to this
  IP, same as the existing setup already verified.
- **Login right after register will 401** until the account is OTP-verified — build the registration
  flow as register → show "check your email" / code-entry screen → verify (which logs in), not
  register → auto-login.
- `POST /api/concept-banks` currently has no auth requirement — anyone can create a bank. Don't rely
  on the UI hiding the button as your only guard if that matters for your flow.
