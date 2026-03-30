package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@RequiredArgsConstructor
public class SqlChatRepository implements ChatRepository {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public void save(Long id) {
        String sql = "INSERT INTO chats (id) VALUES (:id)";
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
    }

    @Override
    public boolean exists(Long id) {
        String sql = "SELECT EXISTS(SELECT 1 FROM chats WHERE id = :id)";
        return namedParameterJdbcTemplate.queryForObject(sql, new MapSqlParameterSource("id", id), Boolean.class);
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM chats WHERE id = :id";
        namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
    }
}
