--liquibase formatted sql
--changeset arinkmm:1
CREATE TABLE chats (
    id BIGINT PRIMARY KEY
);

CREATE TABLE links (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    url TEXT NOT NULL UNIQUE,
    last_checked TIMESTAMPTZ
);

CREATE TABLE subscriptions (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    chat_id BIGINT NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE
);

CREATE TABLE subscription_tags (
    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    tag TEXT NOT NULL,
    PRIMARY KEY (subscription_id, tag)
);
