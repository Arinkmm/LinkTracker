package backend.academy.linktracker.scrapper.configuration;

import org.springframework.jdbc.core.JdbcTemplate;

public final class TestDatabaseCleaner {
    private static final String SCRAPPER_TABLES = "subscriptions, outbox_messages, links, chats";

    public static void clean(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("TRUNCATE TABLE " + SCRAPPER_TABLES + " RESTART IDENTITY CASCADE");
    }

    private TestDatabaseCleaner() {}
}
