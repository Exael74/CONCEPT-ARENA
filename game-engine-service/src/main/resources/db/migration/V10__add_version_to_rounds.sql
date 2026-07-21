-- Optimistic locking (see RoundEntity#version): without it, a stale in-memory Round saved after a
-- concurrent round-transition could silently overwrite the row back to ACTIVE, leaving 2 rounds
-- simultaneously ACTIVE for one room — crashing every subsequent active-round lookup with
-- IncorrectResultSizeDataAccessException (500s on POST /api/game/{roomId}/answer, found in
-- production 2026-07-21). Existing rows default to 0 so Hibernate has a starting version to compare
-- against on their first post-migration update.
ALTER TABLE rounds ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
