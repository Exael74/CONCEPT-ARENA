package com.conceptarena.core.observability.event;

import com.conceptarena.core.shared.event.DomainEvent;

public class MetricsCollected extends DomainEvent {
    public MetricsCollected(String snapshotId) {
        super(snapshotId);
    }
}
