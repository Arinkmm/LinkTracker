package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.UserRepository;
import backend.academy.linktracker.scrapper.repository.jdbc.LinkJdbcAdapter;
import backend.academy.linktracker.scrapper.repository.jdbc.SubscriptionJdbcAdapter;
import backend.academy.linktracker.scrapper.repository.jdbc.UserJdbcAdapter;
import backend.academy.linktracker.scrapper.repository.jpa.JpaLinkRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaSubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaSubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaUserRepository;
import backend.academy.linktracker.scrapper.repository.jpa.LinkJpaAdapter;
import backend.academy.linktracker.scrapper.repository.jpa.SubscriptionJpaAdapter;
import backend.academy.linktracker.scrapper.repository.jpa.UserJpaAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DBConfiguration {
    @Bean
    @ConditionalOnProperty(name = "app.db.access-type", havingValue = "sql", matchIfMissing = true)
    public UserRepository jdbcUserRepository(JdbcTemplate jdbcTemplate) {
        return new UserJdbcAdapter(jdbcTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "app.db.access-type", havingValue = "sql", matchIfMissing = true)
    public LinkRepository jdbcLinkRepository(JdbcTemplate jdbcTemplate) {
        return new LinkJdbcAdapter(jdbcTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "app.db.access-type", havingValue = "sql", matchIfMissing = true)
    public SubscriptionRepository jdbcSubscriptionRepository(JdbcTemplate jdbcTemplate) {
        return new SubscriptionJdbcAdapter(jdbcTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "app.db.access-type", havingValue = "orm")
    public UserRepository userJpaRepository(JpaUserRepository jpaUserRepository) {
        return new UserJpaAdapter(jpaUserRepository);
    }

    @Bean
    @ConditionalOnProperty(name = "app.db.access-type", havingValue = "orm")
    public LinkRepository linkJpaRepository(JpaLinkRepository jpaLinkRepository) {
        return new LinkJpaAdapter(jpaLinkRepository);
    }

    @Bean
    @ConditionalOnProperty(name = "app.db.access-type", havingValue = "orm")
    public SubscriptionRepository subscriptionJpaRepository(
            JpaSubscriptionRepository jpaSubscriptionRepository,
            JpaSubscriptionTagRepository jpaSubscriptionTagRepository) {
        return new SubscriptionJpaAdapter(jpaSubscriptionTagRepository, jpaSubscriptionRepository);
    }
}
