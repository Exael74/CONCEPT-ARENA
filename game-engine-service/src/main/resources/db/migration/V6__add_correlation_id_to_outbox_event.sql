-- Audit gap #5 remediation: correlationId (the originating HTTP request's X-Request-Id) was
-- never carried past the outbox boundary, so logs on the consuming side of RabbitMQ couldn't be
-- linked back to the request that caused them. See docs/event-contracts.md.
ALTER TABLE outbox_event ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(255);
