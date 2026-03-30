package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@RequiredArgsConstructor
@Slf4j
public class SqlLinkRepository implements LinkRepository {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private static final RowMapper<Link> mapper = (rs, r) -> {
        OffsetDateTime odt = rs.getObject("last_checked", OffsetDateTime.class);
        Instant lastChecked = (odt != null) ? odt.toInstant() : null;
        return new Link(rs.getLong("id"), URI.create(rs.getString("url")), lastChecked);
    };

    @Override
    public Link save(URI url) {
        String sql =
                "INSERT INTO links (url) VALUES (:url) ON CONFLICT (url) DO UPDATE SET url = EXCLUDED.url RETURNING id, url, last_checked;";
        return namedParameterJdbcTemplate.queryForObject(sql, new MapSqlParameterSource("url", url.toString()), mapper);
    }

    @Override
    public List<Link> findByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT * FROM links WHERE id = ANY(:ids)";
        return namedParameterJdbcTemplate.query(
                sql, new MapSqlParameterSource("ids", ids.toArray(new Long[0])), mapper);
    }

    @Override
    public Optional<Link> findByUrl(URI url) {
        String sql = "SELECT * FROM links WHERE url = :url";
        try {
            return Optional.of(namedParameterJdbcTemplate.queryForObject(
                    sql, new MapSqlParameterSource("url", url.toString()), mapper));
        } catch (EmptyResultDataAccessException e) {
            log.atWarn().addKeyValue("url", url.toString()).log("No such link found");
            return Optional.empty();
        }
    }

    @Override
    public void remove(Long id) {
        String sql = "DELETE FROM links WHERE id = :id";
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
    }

    @Override
    public void removeIfOrphan(Long id) {
        String sql =
                "DELETE FROM links WHERE id = :id AND NOT EXISTS (SELECT * FROM subscriptions WHERE link_id = :id)";
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
    }

    @Override
    public void removeOrphans() {
        String sql = "DELETE FROM links WHERE id NOT IN (SELECT link_id FROM subscriptions)";
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource());
    }

    @Override
    public List<Link> findStaleLinks(Instant threshold, int page, int size) {
        String sql = """
        SELECT * FROM links
        WHERE last_checked IS NULL OR last_checked < :last_checked
        ORDER BY last_checked NULLS FIRST
        LIMIT :limit OFFSET :offset
        """;
        return namedParameterJdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("last_checked", threshold.atOffset(ZoneOffset.UTC))
                        .addValue("limit", size)
                        .addValue("offset", (long) page * size),
                mapper);
    }

    public void updateLastChecked(Long id, Instant newTime) {
        String sql = "UPDATE links SET last_checked = :last_checked WHERE id = :id";
        namedParameterJdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("last_checked", newTime.atOffset(ZoneOffset.UTC))
                        .addValue("id", id));
    }
}
