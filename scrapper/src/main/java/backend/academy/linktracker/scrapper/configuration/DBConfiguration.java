package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.orm.JpaChatRepository;
import backend.academy.linktracker.scrapper.repository.orm.JpaLinkRepository;
import backend.academy.linktracker.scrapper.repository.orm.JpaSubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.orm.JpaSubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.orm.OrmChatRepository;
import backend.academy.linktracker.scrapper.repository.orm.OrmLinkRepository;
import backend.academy.linktracker.scrapper.repository.orm.OrmSubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.sql.SqlChatRepository;
import backend.academy.linktracker.scrapper.repository.sql.SqlLinkRepository;
import backend.academy.linktracker.scrapper.repository.sql.SqlSubscriptionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
public class DBConfiguration {
    @Bean
    @ConditionalOnProperty(name = "app.db.access-type", havingValue = "sql", matchIfMissing = true)
    public ChatRepository jdbcChatRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        return new SqlChatRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "app.db.access-type", havingValue = "sql", matchIfMissing = true)
    public LinkRepository jdbcLinkRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        return new SqlLinkRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "app.db.access-type", havingValue = "sql", matchIfMissing = true)
    public SubscriptionRepository jdbcSubscriptionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        return new SqlSubscriptionRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "app.db.access-type", havingValue = "orm")
    public ChatRepository chatJpaRepository(JpaChatRepository jpaChatRepository) {
        return new OrmChatRepository(jpaChatRepository);
    }

    @Bean
    @ConditionalOnProperty(name = "app.db.access-type", havingValue = "orm")
    public LinkRepository linkJpaRepository(JpaLinkRepository jpaLinkRepository) {
        return new OrmLinkRepository(jpaLinkRepository);
    }

    @Bean
    @ConditionalOnProperty(name = "app.db.access-type", havingValue = "orm")
    public SubscriptionRepository subscriptionJpaRepository(
            JpaSubscriptionRepository jpaSubscriptionRepository,
            JpaSubscriptionTagRepository jpaSubscriptionTagRepository) {
        return new OrmSubscriptionRepository(jpaSubscriptionTagRepository, jpaSubscriptionRepository);
    }
}
