# Service Level Objectives (SLOs)

Audit gap #3 remediation. Before this file the Prometheus alerts fired on absolute thresholds with
no agreed target behind them, so there was no *error budget* — nothing to say how much failure is
acceptable before we act. These SLOs give each critical endpoint a numeric target; the burn-rate
alerts in `observability/prometheus/alerts.yml` (group `conceptarena-slo-burn-rate`) measure how
fast we are spending the budget.

Definitions: **SLI** = the measured ratio (e.g. good requests / total). **SLO** = the target for
that SLI over a rolling window. **Error budget** = `1 − SLO` (how much we may fail). Windows are
rolling **30 days** unless noted.

## Availability SLOs (per service)

SLI = `1 − (SERVER_ERROR responses / total responses)`, from Micrometer's
`http_server_requests_seconds_count{outcome=...}`.

| Service | Availability SLO | Error budget (30d) | Rationale |
|---|---|---|---|
| api-gateway | 99.9% | 43.2 min/30d | Single entry point — its downtime is everyone's downtime. |
| auth-service | 99.5% | ~3.6 h/30d | Login/registration; degraded auth blocks new sessions, not live games. |
| room-service | 99.9% | 43.2 min/30d | Lobby + room lifecycle; drives every game start. |
| game-engine-service | 99.9% | 43.2 min/30d | The live game path. |
| concept-bank-service | 99.5% | ~3.6 h/30d | Read-mostly; a brief outage delays new banks, not active rounds. |
| voice-signaling-service | 99.0% | ~7.2 h/30d | Best-effort WebRTC relay; voice is an enhancement, not core play. |

## Latency SLOs (per critical endpoint)

SLI = fraction of requests under the threshold, from the `http_server_requests_seconds` histogram
(p95). Targets align with KPI-1/KPI-2 in `CONTEXTO_PROYECTO.md`.

| Endpoint | Method | SLO (p95) | Notes |
|---|---|---|---|
| `/api/game/{roomId}/answer` | POST | **< 500 ms** | Hot path; the answer must feel instant under a full room. |
| `/api/game/{roomId}/start` | POST | < 400 ms | Round start latency (KPI-1: round begins < 200 ms server-side). |
| `/api/rooms` (create) | POST | < 300 ms | |
| `/api/rooms/{id}/join` | POST | < 300 ms | |
| `/api/auth/login` | POST | < 400 ms | Includes BCrypt verification cost. |
| `/api/concept-banks` (create) | POST | < 600 ms | Writes a bank + N concepts + outbox row. |

## How the alerts enforce these

- **Availability** → multi-window burn-rate alerts `SLOFastErrorBudgetBurn` (14.4× over 5m *and* 1h
  → page) and `SLOSlowErrorBudgetBurn` (6× over 30m *and* 6h → ticket), both against the 99.9%
  budget. Two windows must agree, which suppresses flapping on a single spike.
- **Latency** → `HighP95Latency` fires when a service's p95 exceeds 500 ms for 5m.
- **Availability floor** → `ServiceDown` / `RabbitMQDown` page when a target stops being scrapeable.

## Error-budget policy

While a service is over budget (burn-rate alert active), prioritize reliability over features for
that service: no non-critical deploys until the 30-day SLI recovers above target. This is a policy
note, not automation — there is no deploy gate wired up yet.
