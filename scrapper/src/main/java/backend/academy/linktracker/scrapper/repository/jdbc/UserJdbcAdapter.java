package backend.academy.linktracker.scrapper.repository.jdbc;

import backend.academy.linktracker.scrapper.dto.User;
import backend.academy.linktracker.scrapper.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@RequiredArgsConstructor
public class UserJdbcAdapter implements UserRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final RowMapper<User> mapper = (rs, r) -> {
        return new User(rs.getLong("id"));
    };

    @Override
    public void save(Long id) {
        String sql = "INSERT INTO users (id) VALUES (?);";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public boolean exists(Long id) {
        String sql = "SELECT EXISTS(SELECT 1 FROM users WHERE id = ?)";
        return jdbcTemplate.queryForObject(sql, Boolean.class, id);
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM users WHERE id = ?;";
        jdbcTemplate.update(sql, id);
    }
}
