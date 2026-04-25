--liquibase formatted sql
--changeset arinkmm:3
CREATE TABLE IF NOT EXISTS outbox_messages (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    payload JSON NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    retry_count INT DEFAULT 0,
    processed_at TIMESTAMPTZ
);
