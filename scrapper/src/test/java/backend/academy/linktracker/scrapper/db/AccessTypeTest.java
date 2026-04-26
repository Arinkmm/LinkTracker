package backend.academy.linktracker.scrapper.db;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import backend.academy.linktracker.scrapper.configuration.DBConfiguration;
import backend.academy.linktracker.scrapper.configuration.SharedPostgresContainer;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.orm.JpaChatRepository;
import backend.academy.linktracker.scrapper.repository.orm.JpaLinkRepository;
import backend.academy.linktracker.scrapper.repository.orm.JpaOutboxMessageRepository;
import backend.academy.linktracker.scrapper.repository.orm.JpaSubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.orm.JpaSubscriptionTagRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

class AccessTypeTest {
    private static final PostgreSQLContainer<?> postgres = SharedPostgresContainer.INSTANCE;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    JdbcTemplateAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    TransactionAutoConfiguration.class,
                    DBConfiguration.class))
            .withBean(JpaChatRepository.class, () -> Mockito.mock(JpaChatRepository.class))
            .withBean(JpaLinkRepository.class, () -> Mockito.mock(JpaLinkRepository.class))
            .withBean(JpaSubscriptionRepository.class, () -> Mockito.mock(JpaSubscriptionRepository.class))
            .withBean(JpaSubscriptionTagRepository.class, () -> Mockito.mock(JpaSubscriptionTagRepository.class))
            .withBean(JpaOutboxMessageRepository.class, () -> Mockito.mock(JpaOutboxMessageRepository.class))
            .withPropertyValues(
                    "spring.datasource.url=" + postgres.getJdbcUrl(),
                    "spring.datasource.username=" + postgres.getUsername(),
                    "spring.datasource.password=" + postgres.getPassword(),
                    "spring.datasource.driver-class-name=org.postgresql.Driver");

    @ParameterizedTest(name = "access-type={0} → {1}")
    @CsvSource({
        "sql, backend.academy.linktracker.scrapper.repository.sql.SqlLinkRepository",
        "orm, backend.academy.linktracker.scrapper.repository.orm.OrmLinkRepository"
    })
    void shouldUseCorrectRepository(String accessType, Class<?> expectedClass) {
        contextRunner.withPropertyValues("app.db.access-type=" + accessType).run(context -> {
            assertThat(context).hasSingleBean(LinkRepository.class);
            assertThat(context.getBean(LinkRepository.class)).isInstanceOf(expectedClass);
        });
    }
}
