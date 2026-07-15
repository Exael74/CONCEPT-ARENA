// k6 load test: 30 concurrent players joining one room and answering rounds.
//
// This did not exist before (audit finding: "0% de escalabilidad validada empiricamente").
// It measures RPS/p95/p99/error-rate for the REST answer path against a running instance —
// it does NOT by itself fix GameSaga's in-memory-state horizontal-scaling limitation (see
// the class javadoc on GameSaga), it only gives real numbers instead of zero.
//
// Usage (against a locally running `bootstrap` app, default port 8080):
//   k6 run -e BASE_URL=http://localhost:8080 scripts/load/game-30-players.js
//
// Requires k6 (https://k6.io) — not installed as part of this repo/build.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import encoding from 'k6/encoding';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PLAYERS = 30;

export const options = {
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

export function setup() {
    const hostEmail = `load-host-${Date.now()}@escuelaing.edu.co`;
    const hostToken = registerAndLogin(hostEmail);
    const bankId = createConceptBank(hostToken);
    const roomId = createRoom(hostToken, bankId);

    const players = [{ token: hostToken, userId: decodeUserId(hostToken) }];
    for (let i = 1; i < PLAYERS; i++) {
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

    const res = http.post(
        `${BASE_URL}/api/game/${data.roomId}/answer`,
        JSON.stringify({ userId: player.userId, answerText: 'load-test-answer' }),
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

function registerAndLogin(email) {
    http.post(`${BASE_URL}/api/auth/register`, JSON.stringify({ email, password: 'password123' }), {
        headers: { 'Content-Type': 'application/json' },
    });
    const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({ email, password: 'password123' }), {
        headers: { 'Content-Type': 'application/json' },
    });
    return JSON.parse(loginRes.body).data;
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
            userId: decodeUserId(token),
        }),
        { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } }
    );
    return JSON.parse(res.body).data;
}

function joinRoom(token, roomId) {
    http.post(`${BASE_URL}/api/rooms/${roomId}/join`, JSON.stringify({ userId: decodeUserId(token) }), {
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    });
}

// The JWT subject is the userId (see JwtTokenProvider.generateToken) — decode without
// verifying, this is a load-test client, not a security boundary.
function decodeUserId(token) {
    const payload = token.split('.')[1];
    const decoded = JSON.parse(encoding.b64decode(payload.replace(/-/g, '+').replace(/_/g, '/'), 'std', 's'));
    return decoded.sub;
}
