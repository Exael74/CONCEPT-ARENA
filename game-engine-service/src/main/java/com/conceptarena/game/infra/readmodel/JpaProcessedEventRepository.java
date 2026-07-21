package com.conceptarena.game.infra.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;

/** Idempotency ledger of consumed cross-service eventIds — see ProcessedEventEntity (audit gap #6). */
public interface JpaProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
}
