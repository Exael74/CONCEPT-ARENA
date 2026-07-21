-- A5/C10: enforce one session result per (room, user) at the database level, so the idempotency
-- check in SessionResultEventHandler is backed by a real constraint (a concurrent duplicate
-- GameEnded on another replica now fails the second insert instead of double-writing a KPI row).
ALTER TABLE session_results
    ADD CONSTRAINT uq_session_results_room_user UNIQUE (room_id, user_id);
