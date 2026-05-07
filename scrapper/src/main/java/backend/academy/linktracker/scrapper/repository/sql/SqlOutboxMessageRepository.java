package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.repository.OutboxMessageRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.OutboxMessageEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.OutboxStatus;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@RequiredArgsConstructor
public class SqlOutboxMessageRepository implements OutboxMessageRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private static final RowMapper<OutboxMessageEntity> mapper = (rs, r) -> {
        OutboxMessageEntity entity = new OutboxMessageEntity();

        entity.setId(rs.getLong("id"));
        entity.setLinkId(rs.getLong("link_id"));
        entity.setPayload(rs.getString("payload"));
        entity.setRetryCount(rs.getInt("retry_count"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            entity.setStatus(OutboxStatus.valueOf(statusStr));
        }

        OffsetDateTime createdOdt = rs.getObject("created_at", OffsetDateTime.class);
        entity.setCreatedAt(createdOdt != null ? createdOdt.toInstant() : null);

        OffsetDateTime updatedOdt = rs.getObject("updated_at", OffsetDateTime.class);
        entity.setUpdatedAt(updatedOdt != null ? updatedOdt.toInstant() : null);

        return entity;
    };

    @Override
    public void save(OutboxMessageEntity entity) {
        String sql =
                "INSERT INTO outbox_messages (link_id, payload, status, retry_count) VALUES (:linkId, CAST(:payload AS jsonb), :status, :retryCount)";
        namedParameterJdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("linkId", entity.getLinkId())
                        .addValue("payload", entity.getPayload())
                        .addValue("status", entity.getStatus().name())
                        .addValue("retryCount", entity.getRetryCount()));
    }

    @Override
    public void update(OutboxMessageEntity entity) {
        String sql =
                "UPDATE outbox_messages SET status = :status, retry_count = :retryCount, updated_at = :updatedAt WHERE id = :id";
        namedParameterJdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("status", entity.getStatus().name())
                        .addValue("retryCount", entity.getRetryCount())
                        .addValue(
                                "updatedAt",
                                entity.getUpdatedAt() != null ? Timestamp.from(entity.getUpdatedAt()) : null)
                        .addValue("id", entity.getId()));
    }

    @Override
    public List<OutboxMessageEntity> findAll(int maxRetries, int limit) {
        String sql = """
            SELECT * FROM outbox_messages
            WHERE status IN ('NEW', 'ERROR')
              AND retry_count < :maxRetries
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """;
        return namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource().addValue("maxRetries", maxRetries).addValue("limit", limit),
                mapper);
    }

    @Override
    public void deleteOldSentMessages(int daysOld) {
        String sql =
                "DELETE FROM outbox_messages WHERE status = 'SENT' AND updated_at < (NOW() - CAST(:interval AS INTERVAL))";
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource().addValue("interval", daysOld + " DAYS"));
    }
}
