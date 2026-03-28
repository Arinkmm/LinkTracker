package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.dto.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@RequiredArgsConstructor
public class SqlSubscriptionRepository implements SubscriptionRepository {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private static final ResultSetExtractor<List<Subscription>> extractor = rs -> {
        Map<Long, Long> linkToChat = new HashMap<>();
        Map<Long, List<String>> linkToTags = new HashMap<>();

        while (rs.next()) {
            Long chatId = rs.getLong("chat_id");
            Long linkId = rs.getLong("link_id");
            String tag = rs.getString("tag");

            linkToChat.put(linkId, chatId);
            linkToTags.computeIfAbsent(linkId, id -> new ArrayList<>());

            if (tag != null) {
                linkToTags.get(linkId).add(tag);
            }
        }

        return linkToTags.entrySet().stream()
                .map(e -> new Subscription(linkToChat.get(e.getKey()), e.getKey(), List.copyOf(e.getValue())))
                .toList();
    };

    @Override
    public void save(Long chatId, Long linkId, List<String> tags) {
        String sql = "INSERT INTO subscriptions (chat_id, link_id) VALUES (:chat_id, :link_id) RETURNING id";
        Long subscriptionId = namedParameterJdbcTemplate.queryForObject(
                sql, new MapSqlParameterSource().addValue("chat_id", chatId).addValue("link_id", linkId), Long.class);

        if (!tags.isEmpty()) {
            String tagSql =
                    "INSERT INTO subscription_tags (subscription_id, tag) VALUES (:subscription_id, :tag) ON CONFLICT DO NOTHING";
            namedParameterJdbcTemplate.batchUpdate(tagSql, buildTagParams(subscriptionId, tags));
        }
    }

    @Override
    public void remove(Long chatId, Long linkId) {
        String sql = "DELETE FROM subscriptions WHERE chat_id = :chat_id AND link_id = :link_id";
        namedParameterJdbcTemplate.update(
                sql, new MapSqlParameterSource().addValue("chat_id", chatId).addValue("link_id", linkId));
    }

    @Override
    public List<Long> findChatIdByLinkId(Long linkId) {
        String sql = "SELECT chat_id FROM subscriptions WHERE link_id = :link_id";
        return namedParameterJdbcTemplate.queryForList(sql, new MapSqlParameterSource("link_id", linkId), Long.class);
    }

    @Override
    public boolean exists(Long linkId, Long chatId) {
        String sql = "SELECT EXISTS (SELECT 1 FROM subscriptions WHERE link_id = :link_id AND chat_id = :chat_id)";
        return namedParameterJdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource().addValue("link_id", linkId).addValue("chat_id", chatId),
                Boolean.class);
    }

    @Override
    public List<Subscription> findSubscriptionByChatId(Long chatId, int page, int size) {
        String sql = """
        SELECT s.chat_id, s.link_id, t.tag
        FROM subscriptions s
        LEFT JOIN subscription_tags t ON s.id = t.subscription_id
        WHERE s.chat_id = :chat_id
        ORDER BY s.link_id
        LIMIT :limit OFFSET :offset
            """;
        return namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("chat_id", chatId)
                        .addValue("limit", size)
                        .addValue("offset", (long) page * size),
                extractor);
    }

    @Override
    public void saveTag(Long subscriptionId, String tag) {
        String sql =
                "INSERT INTO subscription_tags (subscription_id, tag) VALUES (:subscription_id, :tag) ON CONFLICT DO NOTHING";
        namedParameterJdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("subscription_id", subscriptionId)
                        .addValue("tag", tag));
    }

    @Override
    public void removeTag(Long subscriptionId, String tag) {
        String sql = "DELETE FROM subscription_tags WHERE subscription_id = :subscription_id AND tag = :tag";
        namedParameterJdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("subscription_id", subscriptionId)
                        .addValue("tag", tag));
    }

    @Override
    public List<String> findTags(Long subscriptionId) {
        String sql = "SELECT tag FROM subscription_tags WHERE subscription_id = :subscription_id";
        return namedParameterJdbcTemplate.queryForList(
                sql, new MapSqlParameterSource().addValue("subscription_id", subscriptionId), String.class);
    }

    @Override
    public void updateTags(Long subscriptionId, List<String> tags) {
        String deleteSql = "DELETE FROM subscription_tags WHERE subscription_id = :subscription_id";
        namedParameterJdbcTemplate.update(deleteSql, new MapSqlParameterSource("subscription_id", subscriptionId));

        if (!tags.isEmpty()) {
            String insertSql = "INSERT INTO subscription_tags (subscription_id, tag) VALUES (:subscription_id, :tag)";
            namedParameterJdbcTemplate.batchUpdate(insertSql, buildTagParams(subscriptionId, tags));
        }
    }

    private SqlParameterSource[] buildTagParams(Long subscriptionId, List<String> tags) {
        return tags.stream()
                .map(tag -> (SqlParameterSource) new MapSqlParameterSource()
                        .addValue("subscription_id", subscriptionId)
                        .addValue("tag", tag))
                .toArray(SqlParameterSource[]::new);
    }
}
