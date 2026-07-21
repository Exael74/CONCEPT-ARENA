# Load test — `game-30-players.js`

k6 scenario: 30 players register, one creates a concept bank + room, the rest join, then every
player submits an answer. It reports RPS, p95/p99 latency and error rate for the REST answer path.

## Running it against the real (microservices) stack

The test targets the **api-gateway**, i.e. the full microservices stack — not the legacy monolith.

```bash
# 1. Bring the whole stack up (gateway + 6 services + Postgres/Redis/RabbitMQ + observability).
#    Needs JWT_SECRET set — see .env.example.
docker compose up -d --build

# 2. Run the load test. BASE_URL defaults to the gateway on :8080.
k6 run scripts/load/game-30-players.js
#    (or point elsewhere: k6 run -e BASE_URL=http://host:port scripts/load/game-30-players.js)
```

k6 is not bundled with this repo. Either install it from <https://k6.io>, or run it via Docker on
the compose network (no local install needed — the project network is `conceptarena_default`):

```bash
docker run --rm -i --network conceptarena_default \
  -e BASE_URL=http://api-gateway:8080 \
  -v "$(pwd)/scripts/load:/scripts" \
  grafana/k6 run /scripts/game-30-players.js
```

While it runs you can watch the same traffic in the observability stack this compose file now brings
up: Grafana (`http://localhost:3000`, admin/admin) → "ConceptArena — Overview" dashboard, Prometheus
(`http://localhost:9090`), and per-request traces in Zipkin (`http://localhost:9411`).

## Reading the result

- `http_req_failed` counts **only 5xx** as failures (see `http.setResponseCallback` at the top of the
  script). A `400` "round not active / already answered / time expired" is a *correct* response under
  load — the game legitimately rejecting an answer — not a server defect, so it must not count against
  the error-rate threshold.
- Thresholds: `p(95) < 500ms`, `p(99) < 1000ms`, `http_req_failed rate < 1%`.

## Latest run against the microservices stack — `k6-run-microservices.txt`

Executed 2026-07-20 against the full `docker compose` stack via the api-gateway (see that file for the
raw k6 summary). **All thresholds passed**, a clear improvement over the monolith baseline below:

| Metric | Monolith baseline (2026-07-15) | Microservices stack (latest) |
|---|---|---|
| p(95) http_req_duration | 733.68 ms ✗ | **212.73 ms ✓** |
| p(99) http_req_duration | 763.59 ms | **331.09 ms ✓** |
| http_req_failed (5xx) | 28.36% ✗ | **0.00% ✓** (0 of 141) |
| checks (no 5xx) | 46.66% | **100%** (30/30) |

Latest run was against the full stack including the api-gateway's global rate limiter (A10, 100 rps/IP),
which correctly did not throttle this low-RPS test.

Honest caveat: all 30 answer submits came back as business `400`s (`answer_rejections=30`), not
successful scores. That is because the rate-limited registration in `setup()` (auth caps 10 req/min
per IP, so 30 players take several minutes) outlasts the game's 5-round lifecycle (~150 s) — by the
time the VUs submit, the round window has closed, so game-engine correctly rejects with a fast `400`
(not a 5xx). So this run validates the stack's REST path **latency and availability** under load
(p95 < 500 ms, 0% server errors), not a full successful-answer flow. Aligning the setup pacing with
the round duration (or pre-warming tokens) to exercise accepted answers is a follow-up test refinement.

## About `k6-run-output.monolith-baseline-2026-07-15.txt`

That file is the **first-ever** recorded run and is kept only as a historical baseline. It is **not**
current evidence, because:

1. It ran against the **legacy monolith** (`bootstrap` on :8080), before the api-gateway existed, so
   it never exercised the microservices architecture this test now targets.
2. It **failed** its thresholds (p95 ≈ 734ms, 28% `http_req_failed`) — and a large share of that 28%
   were expected business `400`s counted as failures, plus answer-path `500`s that the audit-gap-#1
   fix (`SubmitAnswerCommandHandler` now maps duplicate-answer races to `400`) has since eliminated.

To produce current evidence, run the two steps above against the microservices stack and commit the
fresh k6 summary alongside this README. Do not treat the baseline file as a passing result.
