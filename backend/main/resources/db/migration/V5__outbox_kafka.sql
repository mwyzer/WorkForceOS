ALTER TABLE outbox_events ADD COLUMN kafka_published_at TIMESTAMP(6) WITH TIME ZONE;
CREATE INDEX idx_outbox_events_kafka_pending ON outbox_events (kafka_published_at);