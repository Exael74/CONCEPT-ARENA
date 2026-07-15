CREATE TABLE IF NOT EXISTS outbox_event (
    id VARCHAR(255) PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    exchange VARCHAR(255) NOT NULL,
    routing_key VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    attempts INT NOT NULL DEFAULT 0,
    last_error TEXT
);

-- A plain (not partial/filtered) index: H2, used for dev/test, doesn't support the
-- "WHERE published_at IS NULL" partial-index syntax Postgres does. Still keeps the poller's
-- "unpublished rows" query fast as the table grows.
CREATE INDEX IF NOT EXISTS idx_outbox_published_at ON outbox_event (published_at);
