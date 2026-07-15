package com.conceptarena.conceptbank.infra.messaging.outbox;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    /**
     * Simplification for a single-poller-instance deployment: no FOR UPDATE SKIP LOCKED yet.
     * ADR-002 documents the intended hardening (native skip-locked query + ShedLock) once this
     * service can run with more than one replica — added when RabbitMQ is wired end-to-end
     * (see the corresponding migration phase in the project plan), not needed before then.
     */
    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
