# Event JSON Schemas (C1)

Formal JSON Schemas for the cross-service events (the ones another service consumes off RabbitMQ —
see `docs/event-contracts.md` for the full routing table). They document the wire contract.

**Enforcement:** the field set of each event is checked in code by `EventContractSerializationTest`
in each producing service (auth, room, concept-bank, game-engine). Those tests fail the build if a
field is renamed/added/removed, so the schemas here and the code cannot silently drift — that was
the open gap ("no schema registry or contract test enforces sync"). The schemas are the
human-readable contract; the tests are the machine-enforced one, both run in CI (`.github/workflows/ci.yml`).

All events also carry the base `DomainEvent` envelope: `eventId` (string), `occurredOn` (ISO-8601
string), `aggregateId` (string).
