package com.conceptarena.core.observability.model;

import java.time.Instant;
import java.util.Map;

public class KpiSnapshot {
    private final String sessionId;
    private final Instant timestamp;
    private final Map<String, Object> metrics;

    public KpiSnapshot(String sessionId, Map<String, Object> metrics) {
        this.sessionId = sessionId;
        this.timestamp = Instant.now();
        this.metrics = metrics;
    }

    public String getSessionId() { return sessionId; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, Object> getMetrics() { return metrics; }
}
