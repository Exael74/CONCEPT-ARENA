-- Idempotency ledger for cross-service events consumed off RabbitMQ (audit gap #6). A row here
-- means "this eventId's effect has already been applied": RoomReadModelEventConsumer checks it
-- before handling RoomJoined/RoomLeft, so an at-least-once redelivery does not re-run the effect —
-- which for those events includes re-publishing on the local bus and re-triggering GameSaga.
-- RoomCreated carries no eventId in its payload and is a natural-key upsert, so it dedups on
-- room_id (room_read_model) instead of on this ledger.
CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(255) PRIMARY KEY,
    consumed_at TIMESTAMP NOT NULL
);
