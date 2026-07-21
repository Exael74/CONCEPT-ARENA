# ADR-012: Business validation lives in the domain/command layer, not controllers

## Status
Accepted

## Context
`GameController.submitAnswer()` does not itself check whether a round is active before dispatching the
command (gap A3); that check happens in `Round.submitAnswer()` (deadline/status) and
`SubmitAnswerCommandHandler` (participant membership, no-active-round). One reading is that the REST
endpoint is "missing" a guard.

## Decision
This is intentional and correct per the project's layering rules. Controllers are thin: they map
HTTP/STOMP to commands, take the authenticated `userId` from the `Principal` (never the body — the A1
class of fix), enforce transport concerns (rate limiting), and translate exceptions to status codes.
All *business* validation — round state, time window, participant membership, one-answer-per-user —
lives in the domain aggregate and the command handler, which is the single place it is enforced
regardless of whether the request arrives over REST or WebSocket.

Duplicating a round-state check in the controller would:
- create a second, drift-prone copy of a business rule, and
- reintroduce exactly the "fat controller" smell the structure review flags elsewhere.

The REST and WS paths already converge on the same `SubmitAnswerCommand` + `Round.submitAnswer()`, so
both are validated identically today.

## Consequences
- No controller-level round guard is added; A3 is resolved as a deliberate layering decision.
- The domain remains the single source of truth for gameplay rules.
