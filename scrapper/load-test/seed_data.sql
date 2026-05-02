INSERT INTO chats (id)
SELECT generate_series(1, 1000);

INSERT INTO links (url, last_checked)
SELECT
    'https://github.com/user/repo-' || generate_series(1, 100000),
    now();

INSERT INTO subscriptions (chat_id, link_id)
SELECT
    ((id - 1) / 100) + 1,
    id
FROM generate_series(1, 100000) AS id;
