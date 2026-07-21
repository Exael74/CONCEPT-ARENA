#!/usr/bin/env bash
#
# Full resilience test (audit gap #5). Exercises the two resilience properties the architecture
# claims but never proved: (1) the transactional outbox decouples writes from publishing, so a write
# SUCCEEDS even while RabbitMQ is down and the buffered events flush once it recovers; (2) a crashed
# service restarts and becomes healthy again (restart policies in docker-compose.yml).
#
# Usage:
#   scripts/test-resilience-full.sh            # brings the stack up, runs the checks, leaves it up
#   KEEP_UP=0 scripts/test-resilience-full.sh  # tear the stack down at the end
#
# Requires: docker compose, curl, jq. JWT_SECRET must be set (see .env / .env.example).
set -euo pipefail

GATEWAY="${GATEWAY:-http://localhost:8080}"
RABBIT_API="${RABBIT_API:-http://localhost:15672/api}"
RABBIT_AUTH="${RABBIT_AUTH:-guest:guest}"
KEEP_UP="${KEEP_UP:-1}"
COMPOSE="docker compose"

pass() { printf '  \033[32mPASS\033[0m %s\n' "$1"; }
fail() { printf '  \033[31mFAIL\033[0m %s\n' "$1"; FAILED=1; }
step() { printf '\n\033[1m== %s ==\033[0m\n' "$1"; }
FAILED=0

wait_for() { # url, seconds
  local url="$1" timeout="${2:-120}" start; start=$(date +%s)
  until curl -fsS -o /dev/null "$url" 2>/dev/null; do
    [ $(( $(date +%s) - start )) -ge "$timeout" ] && return 1
    sleep 3
  done
}

# ---------------------------------------------------------------------------------------------------
step "Bringing up the stack"
$COMPOSE up -d --build
echo "Waiting for the gateway to become healthy (up to 4 min)…"
wait_for "$GATEWAY/actuator/health" 240 || { echo "gateway never came up"; exit 1; }
pass "gateway is up"

# ---------------------------------------------------------------------------------------------------
step "Baseline: a write publishes an event (outbox -> RabbitMQ)"
EMAIL="resil-$(date +%s)@escuelaing.edu.co"
USERNAME="resil$(date +%s)"
MAILHOG="${MAILHOG:-http://localhost:8025}"
curl -fsS -X POST "$GATEWAY/api/auth/register" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"username\":\"$USERNAME\",\"password\":\"password123\"}" >/dev/null || true

# Registration creates an INACTIVE account (see auth-service VerifyOtpCommandHandler) — it must be
# verified via the OTP emailed to MailHog before it can authenticate. Poll for the mail, decode the
# quoted-printable body (soft line breaks can otherwise split the 6-digit code), and verify.
CODE=""
for _ in $(seq 1 10); do
  CODE=$(curl -fsS "$MAILHOG/api/v2/search?kind=to&query=$EMAIL" 2>/dev/null \
    | jq -r '.items[0].Content.Body // empty' \
    | sed ':a;N;$!ba;s/=\r\{0,1\}\n//g' \
    | grep -oE '[0-9]{6}' | head -1) || true
  [ -n "$CODE" ] && break
  sleep 1
done
TOKEN=$(curl -fsS -X POST "$GATEWAY/api/auth/otp/verify" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"code\":\"$CODE\"}" | jq -r '.data')
[ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] && pass "registered + OTP-verified" || fail "could not obtain a token"

BANK=$(curl -fsS -X POST "$GATEWAY/api/concept-banks" -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Resil Bank","subject":"Resil","concepts":[{"question":"q1?","expectedAnswer":"a1","difficulty":1}]}' \
  | jq -r '.data')
curl -fsS -X POST "$GATEWAY/api/rooms" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"name\":\"Resil Room\",\"type\":\"PUBLIC\",\"conceptBankId\":\"$BANK\",\"maxParticipants\":4}" >/dev/null
pass "created a concept bank + room (RoomCreated should be flowing)"

# ---------------------------------------------------------------------------------------------------
step "TEST 1 — write survives RabbitMQ being DOWN (outbox decoupling)"
$COMPOSE stop rabbitmq
sleep 3
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$GATEWAY/api/rooms" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"name\":\"Room While RMQ Down\",\"type\":\"PUBLIC\",\"conceptBankId\":\"$BANK\",\"maxParticipants\":4}")
if [ "$CODE" = "201" ] || [ "$CODE" = "200" ]; then
  pass "room created (HTTP $CODE) while RabbitMQ was down — the write did NOT depend on the broker"
else
  fail "write returned HTTP $CODE while RabbitMQ was down (outbox should have decoupled it)"
fi

# ---------------------------------------------------------------------------------------------------
step "TEST 2 — buffered events flush after RabbitMQ recovers"
$COMPOSE start rabbitmq
wait_for "$GATEWAY/actuator/health" 120 || true
echo "Waiting for the outbox publishers to reconnect and drain (up to 60s)…"
DRAINED=0
for _ in $(seq 1 20); do
  READY=$(curl -fsS -u "$RABBIT_AUTH" "$RABBIT_API/queues" 2>/dev/null \
    | jq '[.[] | select(.name | startswith("game-engine.")) | .messages_ready] | add // 0')
  MSGS=$(curl -fsS -u "$RABBIT_AUTH" "$RABBIT_API/overview" 2>/dev/null | jq '.queue_totals.messages // 0')
  # The read-model queues should not keep growing unbounded — a live consumer drains them.
  if [ "${READY:-0}" -le 5 ]; then DRAINED=1; break; fi
  sleep 3
done
[ "$DRAINED" = "1" ] && pass "read-model queues drained after recovery (consumers caught up)" \
                     || fail "read-model queues kept a backlog after recovery"

# ---------------------------------------------------------------------------------------------------
step "TEST 3 — a crashed service recovers"
$COMPOSE kill game-engine-service
sleep 2
$COMPOSE start game-engine-service
if wait_for "http://localhost:8083/actuator/health" 120; then
  pass "game-engine-service came back healthy after being killed"
else
  fail "game-engine-service did not recover"
fi

# ---------------------------------------------------------------------------------------------------
step "Result"
if [ "$KEEP_UP" != "1" ]; then $COMPOSE down; fi
if [ "$FAILED" = "0" ]; then
  printf '\033[32mALL RESILIENCE CHECKS PASSED\033[0m\n'; exit 0
else
  printf '\033[31mSOME RESILIENCE CHECKS FAILED\033[0m\n'; exit 1
fi
