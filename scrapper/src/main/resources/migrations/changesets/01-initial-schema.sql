--liquibase formatted sql
--changeset arinkmm:1
CREATE TABLE users (
    id BIGINT PRIMARY KEY
);

CREATE TABLE links (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    url TEXT NOT NULL UNIQUE,
    last_checked TIMESTAMPTZ
);

CREATE TABLE subscriptions (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, link_id)
);

CREATE TABLE subscription_tags (
    user_id BIGINT NOT NULL,
    link_id BIGINT NOT NULL,
    tag TEXT NOT NULL,
    PRIMARY KEY (user_id, link_id, tag),
    FOREIGN KEY (user_id, link_id) REFERENCES subscriptions(user_id, link_id) ON DELETE CASCADE
);
