// k6 load test: 150 concurrent players joining one room and playing all 5 rounds.
//
// Targets the FULL microservices stack through the api-gateway (docker-compose.yml) — not the legacy
// monolith the first recorded run used. The default BASE_URL is the gateway's published port, and
// with the docker profile active game-engine-service runs on RedisGameStateStore/RedisRoundEndGuard,
// so this exercises the externalized-state path the architecture actually ships (audit gap #3), not
// an in-memory single instance. It measures RPS / p95 / p99 / error-rate for the REST answer path
// across all 5 rounds of a game.
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
const MAILHOG_URL = __ENV.MAILHOG_URL || 'http://localhost:8025';
const PLAYERS = 150;
const ROUNDS = 5;
const ROUND_DURATION_SECONDS = 30;

// http_req_failed must flag SERVER failures (5xx), not the game's legitimate business rejections: a
// 400 "round not active / already answered / time expired" is a CORRECT response under load, not a
// defect. Treating 200–499 as expected aligns the failure metric with the per-request check below.
// (Audit gap #3: the earlier monolith run counted those expected 400s as failures, muddying the
// signal — and answer-race 500s are now 400s too, see SubmitAnswerCommandHandler.)
http.setResponseCallback(http.expectedStatuses({ min: 200, max: 499 }));

export const options = {
    // Found by actually running this script for the first time (audit gap #2 remediation,
    // 2026-07-15): setup() registers/logs in PLAYERS (now 150) players sequentially from k6's
    // single client IP, and auth-service's RateLimitingFilter caps /api/auth/{register,login}
    // combined at 10 req/min per IP (a real, working anti-abuse control, not a bug — see
    // RateLimitingFilter.java). At 2 requests/player that's 5 players/window; setup below paces
    // itself to stay under it, so setup alone takes ~16 min — bump the default
    // 60s setupTimeout accordingly.
    setupTimeout: '30m',
    scenarios: {
        one_fifty_players: {
            executor: 'per-vu-iterations',
            vus: PLAYERS,
            iterations: 1,
            maxDuration: '45m',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],
        http_req_failed: ['rate<0.01'],
    },
};

const answerRejections = new Counter('answer_rejections');
const answerLatency = new Trend('answer_submit_duration_ms');
const roundStartLatency = new Trend('round_start_latency_ms');

// Fixed-window rate limit is 10 req/min per IP shared across register+login (2 req/player) — 6.5s
// between players keeps every window under that cap with margin, even near a window boundary.
const SECONDS_BETWEEN_PLAYER_SETUPS = 6.5;

export function setup() {
    const hostEmail = `load-host-${Date.now()}@escuelaing.edu.co`;
    const hostToken = registerAndLogin(hostEmail);
    const bankId = createConceptBank(hostToken);
    const roomId = createRoom(hostToken, bankId);

    const players = [{ token: hostToken }];
    for (let i = 1; i < PLAYERS; i++) {
        sleep(SECONDS_BETWEEN_PLAYER_SETUPS);
        const email = `load-player-${i}-${Date.now()}@escuelaing.edu.co`;
        const token = registerAndLogin(email);
        players.push({ token });
    }

    return { roomId, players };
}

export default function (data) {
    const player = data.players[__VU - 1];
    if (!player) return;
    // players[0] is the room creator (see setup) — VU numbering is 1-based, so VU 1 is the host.
    const isHost = __VU === 1;

    // The host already joined by creating the room; the rest join now.
    if (!isHost) {
        http.post(`${BASE_URL}/api/rooms/${data.roomId}/join`, null, {
            headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${player.token}` },
        });
    }

    // The game no longer auto-starts on the 2nd join (changed 2026-07-21) — only the room creator
    // can start it (POST /api/game/{roomId}/start). Host waits ~3s for everyone to join, then starts;
    // the others begin answering a beat later so round 1 is active by the time they submit.
    if (isHost) {
        sleep(3);
        http.post(`${BASE_URL}/api/game/${data.roomId}/start`, null, {
            headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${player.token}` },
        });
    } else {
        sleep(4);
    }

    for (let round = 0; round < ROUNDS; round++) {
        const t0 = Date.now();
        const res = http.post(
            `${BASE_URL}/api/game/${data.roomId}/answer`,
            JSON.stringify({ answerText: 'load-test-answer' }),
            {
                headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${player.token}` },
            }
        );
        roundStartLatency.add(Date.now() - t0);

        answerLatency.add(res.timings.duration);
        const ok = check(res, { 'answer submit is 2xx or 4xx (not 5xx)': (r) => r.status < 500 });
        if (!ok || res.status >= 400) {
            answerRejections.add(1);
        }

        // Wait for the next round (30s per round). On the last iteration skip the sleep.
        if (round < ROUNDS - 1) {
            sleep(ROUND_DURATION_SECONDS);
        }
    }
}

// Retries on 429 instead of relying purely on the SECONDS_BETWEEN_PLAYER_SETUPS pacing to land
// outside the rate limiter's window: a fixed-window limiter's reset boundary doesn't line up
// predictably with evenly-spaced sleeps (found by actually running this — pacing alone still hit
// 429s intermittently near a window edge), so treat 429 as expected/retryable, not fatal.
// Registration now requires a username and creates an INACTIVE account (see auth-service
// VerifyOtpCommandHandler) — it must be OTP-verified before it can authenticate. Verify returns a
// JWT directly, so this polls MailHog for the emailed code instead of calling /api/auth/login.
function fetchOtpCode(email) {
    for (let attempt = 0; attempt < 10; attempt++) {
        const res = http.get(`${MAILHOG_URL}/api/v2/search?kind=to&query=${email}`);
        if (res.status === 200) {
            const body = JSON.parse(res.body);
            const raw = body.items && body.items[0] && body.items[0].Content && body.items[0].Content.Body;
            if (raw) {
                const decoded = raw.replace(/=\r?\n/g, '');
                const match = decoded.match(/\d{6}/);
                if (match) return match[0];
            }
        }
        sleep(1);
    }
    throw new Error(`fetchOtpCode(${email}): no OTP email found in MailHog after 10 attempts`);
}

function registerAndLogin(email) {
    const username = `p${Date.now()}${Math.floor(Math.random() * 1000)}`.slice(0, 20);
    for (let attempt = 0; attempt < 5; attempt++) {
        http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({ email, username, password: 'password123' }), {
            headers: { 'Content-Type': 'application/json' },
        });
        const code = fetchOtpCode(email);
        const loginRes = http.post(`${BASE_URL}/api/auth/otp/verify`, JSON.stringify({ email, code }), {
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

// The JWT subject is the userId (see JwtTokenProvider.generateToken) — decode without
// verifying, this is a load-test client, not a security boundary.
function decodeUserId(token) {
    const payload = token.split('.')[1];
    const decoded = JSON.parse(encoding.b64decode(payload, 'rawurl', 's'));
    return decoded.sub;
}
