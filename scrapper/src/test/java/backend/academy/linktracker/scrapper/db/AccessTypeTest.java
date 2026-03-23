package backend.academy.linktracker.scrapper.db;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import backend.academy.linktracker.scrapper.configuration.DBConfiguration;
import backend.academy.linktracker.scrapper.configuration.DatabaseIntegrationEnvironment;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.jdbc.LinkJdbcAdapter;
import backend.academy.linktracker.scrapper.repository.jpa.JpaLinkRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaSubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaSubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.jpa.JpaUserRepository;
import backend.academy.linktracker.scrapper.repository.jpa.LinkJpaAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import(DatabaseIntegrationEnvironment.LiquibaseConfig.class)
class AccessTypeTest extends DatabaseIntegrationEnvironment {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    JdbcTemplateAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    TransactionAutoConfiguration.class,
                    DBConfiguration.class))
            .withBean(JpaUserRepository.class, () -> Mockito.mock(JpaUserRepository.class))
            .withBean(JpaLinkRepository.class, () -> Mockito.mock(JpaLinkRepository.class))
            .withBean(JpaSubscriptionRepository.class, () -> Mockito.mock(JpaSubscriptionRepository.class))
            .withBean(JpaSubscriptionTagRepository.class, () -> Mockito.mock(JpaSubscriptionTagRepository.class));

    @Test
    void shouldRegisterJdbcBean() {
        contextRunner
                .withPropertyValues(
                        "app.db.access-type=sql",
                        "spring.datasource.url=" + postgres.getJdbcUrl(),
                        "spring.datasource.username=" + postgres.getUsername(),
                        "spring.datasource.password=" + postgres.getPassword(),
                        "spring.datasource.driver-class-name=org.postgresql.Driver")
                .run(context -> {
                    assertThat(context).hasSingleBean(LinkRepository.class);
                    assertThat(context.getBean(LinkRepository.class)).isInstanceOf(LinkJdbcAdapter.class);
                });
    }

    @Test
    void shouldSwitchToJpaBean() {
        contextRunner
                .withPropertyValues(
                        "app.db.access-type=orm",
                        "spring.datasource.url=" + postgres.getJdbcUrl(),
                        "spring.datasource.username=" + postgres.getUsername(),
                        "spring.datasource.password=" + postgres.getPassword(),
                        "spring.datasource.driver-class-name=org.postgresql.Driver")
                .run(context -> {
                    assertThat(context).hasSingleBean(LinkRepository.class);
                    assertThat(context.getBean(LinkRepository.class)).isInstanceOf(LinkJpaAdapter.class);
                });
    }
}
