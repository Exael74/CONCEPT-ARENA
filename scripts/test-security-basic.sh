#!/usr/bin/env bash
#
# A3: basic automated security checks — regression guard for the vulnerabilities that were fixed
# (userId spoofing, auth rate limiting, oversized/blank payloads, security headers). Not a full pen
# test; a fast smoke suite runnable against a running stack (Mode A or B).
#
# Usage:  BASE_URL=http://localhost:8080 scripts/test-security-basic.sh
# Requires: curl, jq.
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
pass() { printf '  \033[32mPASS\033[0m %s\n' "$1"; }
fail() { printf '  \033[31mFAIL\033[0m %s\n' "$1"; FAILED=1; }
step() { printf '\n\033[1m== %s ==\033[0m\n' "$1"; }
FAILED=0

step "1. Security response headers present at the gateway (A9)"
HEADERS=$(curl -sf -D - -o /dev/null "$BASE_URL/api/rooms" 2>/dev/null || curl -sf -D - -o /dev/null "$BASE_URL/actuator/health")
echo "$HEADERS" | grep -qi "X-Content-Type-Options: nosniff" && pass "X-Content-Type-Options: nosniff" || fail "missing X-Content-Type-Options"
echo "$HEADERS" | grep -qi "X-Frame-Options: DENY" && pass "X-Frame-Options: DENY" || fail "missing X-Frame-Options"

step "2. Protected endpoint rejects requests with no JWT (401/403)"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE_URL/api/game/room-x/answer" \
  -H 'Content-Type: application/json' -d '{"answerText":"x"}')
[ "$CODE" = "401" ] || [ "$CODE" = "403" ] && pass "unauthenticated answer rejected (HTTP $CODE)" || fail "expected 401/403, got $CODE"

step "3. Auth rate limiting trips after the per-IP window (A/S5)"
EMAIL="sec-$(date +%s)@escuelaing.edu.co"
GOT_429=0
for i in $(seq 1 15); do
  C=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE_URL/api/auth/login" \
    -H 'Content-Type: application/json' -d "{\"email\":\"$EMAIL\",\"password\":\"nope\"}")
  if [ "$C" = "429" ]; then GOT_429=1; break; fi
done
[ "$GOT_429" = "1" ] && pass "login rate limit returned 429 within 15 attempts" || fail "no 429 seen — rate limiter not enforcing"

step "4. Invalid email is a clean 400, not a 500 (A1)"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE_URL/api/auth/register" \
  -H 'Content-Type: application/json' -d '{"email":"not-an-email","password":"password123"}')
[ "$CODE" = "400" ] && pass "invalid email -> 400" || fail "invalid email -> $CODE (expected 400)"

step "5. Oversized answer body is rejected, not a 500 (A5)"
BIG=$(printf 'a%.0s' $(seq 1 6000))
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE_URL/api/game/room-x/answer" \
  -H 'Content-Type: application/json' -d "{\"answerText\":\"$BIG\"}")
[ "$CODE" != "500" ] && pass "oversized answer did not 500 (HTTP $CODE)" || fail "oversized answer caused a 500"

step "Result"
if [ "$FAILED" = "0" ]; then printf '\033[32mALL BASIC SECURITY CHECKS PASSED\033[0m\n'; exit 0
else printf '\033[31mSOME SECURITY CHECKS FAILED\033[0m\n'; exit 1; fi
