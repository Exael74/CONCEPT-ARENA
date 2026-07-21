// k6 load test: 30 concurrent players joining one room and answering rounds.
//
// Targets the FULL microservices stack through the api-gateway (docker-compose.yml) — not the legacy
// monolith the first recorded run used. The default BASE_URL is the gateway's published port, and
// with the docker profile active game-engine-service runs on RedisGameStateStore/RedisRoundEndGuard,
// so this exercises the externalized-state path the architecture actually ships (audit gap #3), not
// an in-memory single instance. It measures RPS / p95 / p99 / error-rate for the REST answer path.
//
// Usage:
//   1. docker compose up -d --build            # gateway + 6 services + infra (+ observability)
//   2. k6 run scripts/load/game-30-players.js  # BASE_URL defaults to the gateway on :8080
//
// See scripts/load/README.md for how to interpret the result and regenerate the recorded output.
// Requires k6 (https://k6.io) — not installed as part of this repo/build.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import encoding from 'k6/encoding';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PLAYERS = 30;

// http_req_failed must flag SERVER failures (5xx), not the game's legitimate business rejections: a
// 400 "round not active / already answered / time expired" is a CORRECT response under load, not a
// defect. Treating 200–499 as expected aligns the failure metric with the per-request check below.
// (Audit gap #3: the earlier monolith run counted those expected 400s as failures, muddying the
// signal — and answer-race 500s are now 400s too, see SubmitAnswerCommandHandler.)
http.setResponseCallback(http.expectedStatuses({ min: 200, max: 499 }));

export const options = {
    // Found by actually running this script for the first time (audit gap #2 remediation,
    // 2026-07-15): setup() registers/logs in 30 players sequentially from k6's single client
    // IP, and auth-service's RateLimitingFilter caps /api/auth/{register,login} combined at 10
    // req/min per IP (a real, working anti-abuse control, not a bug — see
    // RateLimitingFilter.java). At 2 requests/player that's 5 players/window; setup below paces
    // itself to stay under it, so setup alone now takes minutes, not seconds — bump the default
    // 60s setupTimeout accordingly.
    setupTimeout: '10m',
    scenarios: {
        thirty_players: {
            executor: 'per-vu-iterations',
            vus: PLAYERS,
            iterations: 1,
            maxDuration: '2m',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],
        http_req_failed: ['rate<0.01'],
    },
};

const answerRejections = new Counter('answer_rejections');
const answerLatency = new Trend('answer_submit_duration_ms');

// Fixed-window rate limit is 10 req/min per IP shared across register+login (2 req/player) — 6.5s
// between players keeps every window under that cap with margin, even near a window boundary.
const SECONDS_BETWEEN_PLAYER_SETUPS = 6.5;

export function setup() {
    const hostEmail = `load-host-${Date.now()}@escuelaing.edu.co`;
    const hostToken = registerAndLogin(hostEmail);
    const bankId = createConceptBank(hostToken);
    const roomId = createRoom(hostToken, bankId);

    const players = [{ token: hostToken, userId: decodeUserId(hostToken) }];
    for (let i = 1; i < PLAYERS; i++) {
        sleep(SECONDS_BETWEEN_PLAYER_SETUPS);
        const email = `load-player-${i}-${Date.now()}@escuelaing.edu.co`;
        const token = registerAndLogin(email);
        joinRoom(token, roomId);
        players.push({ token, userId: decodeUserId(token) });
    }

    return { roomId, players };
}

export default function (data) {
    const player = data.players[__VU - 1];
    if (!player) return;

    sleep(1); // let the round auto-start once all participants have joined

    // userId is no longer sent — GameController.submitAnswer takes it from the authenticated
    // principal (JWT), not the request body (audit gap #1 remediation).
    const res = http.post(
        `${BASE_URL}/api/game/${data.roomId}/answer`,
        JSON.stringify({ answerText: 'load-test-answer' }),
        {
            headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${player.token}` },
        }
    );

    answerLatency.add(res.timings.duration);
    const ok = check(res, { 'answer submit is 2xx or 4xx (not 5xx)': (r) => r.status < 500 });
    if (!ok || res.status >= 400) {
        answerRejections.add(1);
    }
}

// Retries on 429 instead of relying purely on the SECONDS_BETWEEN_PLAYER_SETUPS pacing to land
// outside the rate limiter's window: a fixed-window limiter's reset boundary doesn't line up
// predictably with evenly-spaced sleeps (found by actually running this — pacing alone still hit
// 429s intermittently near a window edge), so treat 429 as expected/retryable, not fatal.
function registerAndLogin(email) {
    for (let attempt = 0; attempt < 5; attempt++) {
        http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({ email, password: 'password123' }), {
            headers: { 'Content-Type': 'application/json' },
        });
        const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({ email, password: 'password123' }), {
            headers: { 'Content-Type': 'application/json' },
        });
        if (loginRes.status === 429) {
            sleep(15); // outlast the 60s fixed window without guessing exactly where it resets
            continue;
        }
        const token = JSON.parse(loginRes.body).data;
        if (!token) {
            throw new Error(`registerAndLogin(${email}) got unexpected response ${loginRes.status}: ${loginRes.body}`);
        }
        return token;
    }
    throw new Error(`registerAndLogin(${email}) still rate-limited after 5 attempts`);
}

function createConceptBank(token) {
    const concepts = [1, 2, 3, 4, 5].map((n) => ({
        question: `Load test concept ${n}?`,
        expectedAnswer: `answer-${n}`,
        difficulty: 1,
    }));
    const res = http.post(
        `${BASE_URL}/api/concept-banks`,
        JSON.stringify({ name: 'Load Test Bank', subject: 'Load', concepts }),
        { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } }
    );
    return JSON.parse(res.body).data;
}

function createRoom(token, bankId) {
    // userId is no longer sent — RoomController.createRoom now takes the creator's userId from
    // the authenticated principal (JWT), not the request body (audit gap #1's class of bug,
    // found by extension in RoomController while validating GameController's fix).
    const res = http.post(
        `${BASE_URL}/api/rooms`,
        JSON.stringify({
            name: 'Load Test Room',
            type: 'PUBLIC',
            conceptBankId: bankId,
            maxParticipants: PLAYERS,
        }),
        { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } }
    );
    return JSON.parse(res.body).data;
}

function joinRoom(token, roomId) {
    // No body needed — same principal-based userId fix as createRoom above.
    http.post(`${BASE_URL}/api/rooms/${roomId}/join`, null, {
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    });
}

// The JWT subject is the userId (see JwtTokenProvider.generateToken) — decode without
// verifying, this is a load-test client, not a security boundary.
//
// Bug found by actually running this script for the first time (audit gap #2 remediation,
// 2026-07-15): manually swapping base64url's "-"/"_" for "+"/"/" and decoding as 'std' fails
// because JWT segments have no "=" padding, which 'std' mode requires — k6's 'rawurl' encoding
// handles base64url (unpadded) directly, no manual char-swapping needed.
function decodeUserId(token) {
    const payload = token.split('.')[1];
    const decoded = JSON.parse(encoding.b64decode(payload, 'rawurl', 's'));
    return decoded.sub;
}
