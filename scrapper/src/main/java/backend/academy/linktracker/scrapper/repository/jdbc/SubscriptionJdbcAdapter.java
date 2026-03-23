package backend.academy.linktracker.scrapper.repository.jdbc;

import backend.academy.linktracker.scrapper.dto.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

@RequiredArgsConstructor
public class SubscriptionJdbcAdapter implements SubscriptionRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final ResultSetExtractor<List<Subscription>> extractor = rs -> {
        Map<Long, Long> linkToUser = new HashMap<>();
        Map<Long, List<String>> linkToTags = new HashMap<>();

        while (rs.next()) {
            Long userId = rs.getLong("user_id");
            Long linkId = rs.getLong("link_id");
            String tag = rs.getString("tag");

            linkToUser.put(linkId, userId);
            linkToTags.computeIfAbsent(linkId, id -> new ArrayList<>());

            if (tag != null) {
                linkToTags.get(linkId).add(tag);
            }
        }

        return linkToTags.entrySet().stream()
                .map(e -> new Subscription(linkToUser.get(e.getKey()), e.getKey(), List.copyOf(e.getValue())))
                .toList();
    };

    @Override
    public void save(Long userId, Long linkId, List<String> tags) {
        String sql = "INSERT INTO subscriptions (user_id, link_id) VALUES (?, ?);";
        jdbcTemplate.update(sql, userId, linkId);

        if (!tags.isEmpty()) {
            String tagSql =
                    "INSERT INTO subscription_tags (user_id, link_id, tag) VALUES (?, ?, ?) ON CONFLICT DO NOTHING";
            jdbcTemplate.batchUpdate(tagSql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setLong(1, userId);
                    ps.setLong(2, linkId);
                    ps.setString(3, tags.get(i));
                }

                @Override
                public int getBatchSize() {
                    return tags.size();
                }
            });
        }
    }

    @Override
    public void remove(Long userId, Long linkId) {
        String sql = "DELETE FROM subscriptions WHERE user_id=? AND link_id=?";
        jdbcTemplate.update(sql, userId, linkId);
    }

    @Override
    public List<Long> findUserIdByLinkId(Long linkId) {
        String sql = "SELECT user_id FROM subscriptions WHERE link_id=?";
        return jdbcTemplate.queryForList(sql, Long.class, linkId);
    }

    @Override
    public boolean exists(Long linkId, Long userId) {
        String sql = "SELECT EXISTS (SELECT 1 FROM subscriptions WHERE link_id = ? AND user_id = ?);";
        return jdbcTemplate.queryForObject(sql, Boolean.class, linkId, userId);
    }

    @Override
    public List<Subscription> findSubscriptionByUserId(Long userId, int page, int size) {
        String sql = """
            SELECT s.user_id, s.link_id, t.tag
            FROM subscriptions s
            LEFT JOIN subscription_tags t
                ON s.user_id = t.user_id AND s.link_id = t.link_id
            WHERE s.user_id = ? ORDER BY s.link_id LIMIT ? OFFSET ?;
            """;
        return jdbcTemplate.query(sql, extractor, userId, size, (long) page * size);
    }

    @Override
    public List<Long> findLinkIdByUserId(Long userId) {
        String sql = "SELECT link_id FROM subscriptions WHERE user_id = ?";
        return jdbcTemplate.queryForList(sql, Long.class, userId);
    }

    @Override
    public void saveTag(Long userId, Long linkId, String tag) {
        String sql = "INSERT INTO subscription_tags (user_id, link_id, tag) VALUES (?, ?, ?) ON CONFLICT DO NOTHING;";
        jdbcTemplate.update(sql, userId, linkId, tag);
    }

    @Override
    public void removeTag(Long userId, Long linkId, String tag) {
        String sql = "DELETE FROM subscription_tags WHERE link_id = ? AND user_id = ? AND tag = ?;";
        jdbcTemplate.update(sql, linkId, userId, tag);
    }

    @Override
    public List<String> findTags(Long userId, Long linkId) {
        String sql = "SELECT tag FROM subscription_tags WHERE link_id = ? AND user_id = ?;";
        return jdbcTemplate.queryForList(sql, String.class, linkId, userId);
    }

    @Override
    public void updateTags(Long userId, Long linkId, List<String> tags) {
        String deleteSql = "DELETE FROM subscription_tags WHERE user_id = ? AND link_id = ?";
        jdbcTemplate.update(deleteSql, userId, linkId);

        if (!tags.isEmpty()) {
            String insertSql = "INSERT INTO subscription_tags (user_id, link_id, tag) VALUES (?, ?, ?)";
            jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setLong(1, userId);
                    ps.setLong(2, linkId);
                    ps.setString(3, tags.get(i));
                }

                @Override
                public int getBatchSize() {
                    return tags.size();
                }
            });
        }
    }
}
