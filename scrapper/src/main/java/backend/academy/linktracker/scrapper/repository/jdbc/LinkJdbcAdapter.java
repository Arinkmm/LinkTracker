package backend.academy.linktracker.scrapper.repository.jdbc;

import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@RequiredArgsConstructor
public class LinkJdbcAdapter implements LinkRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final RowMapper<Link> mapper = (rs, r) -> {
        OffsetDateTime odt = rs.getObject("last_checked", OffsetDateTime.class);
        Instant lastChecked = (odt != null) ? odt.toInstant() : null;
        return new Link(rs.getLong("id"), URI.create(rs.getString("url")), lastChecked);
    };

    @Override
    public Link save(URI url) {
        String sql =
                "INSERT INTO links (url) VALUES (?) ON CONFLICT (url) DO UPDATE SET url = EXCLUDED.url RETURNING id, url, last_checked;";
        return jdbcTemplate.queryForObject(sql, mapper, url.toString());
    }

    @Override
    public List<Link> findByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT * FROM links WHERE id = ANY(?)";
        return jdbcTemplate.query(
                con -> {
                    PreparedStatement ps = con.prepareStatement(sql);
                    Array array = con.createArrayOf("bigint", ids.toArray());
                    ps.setArray(1, array);
                    return ps;
                },
                mapper);
    }

    @Override
    public Optional<Link> findByUrl(URI url) {
        String sql = "SELECT * FROM links WHERE url = ?";
        try {
            return Optional.of(jdbcTemplate.queryForObject(sql, mapper, url.toString()));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void remove(Long id) {
        String sql = "DELETE FROM links WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public List<Link> findStaleLinks(Instant threshold, int page, int size) {
        String sql = """
        SELECT * FROM links
        WHERE last_checked IS NULL OR last_checked < ?
        ORDER BY last_checked NULLS FIRST
        LIMIT ? OFFSET ?
        """;
        return jdbcTemplate.query(sql, mapper, threshold.atOffset(ZoneOffset.UTC), size, (long) page * size);
    }

    public void updateLastChecked(Long id, Instant newTime) {
        String sql = "UPDATE links SET last_checked = ? WHERE id = ?";
        jdbcTemplate.update(sql, newTime.atOffset(ZoneOffset.UTC), id);
    }
}
