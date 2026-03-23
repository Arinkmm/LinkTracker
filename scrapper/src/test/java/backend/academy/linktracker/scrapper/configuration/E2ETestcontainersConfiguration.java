package backend.academy.linktracker.scrapper.configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

@TestConfiguration(proxyBeanMethods = false)
public class E2ETestcontainersConfiguration {

    @Bean
    public Network e2eNetwork() {
        return Network.newNetwork();
    }

    @Bean
    public PostgreSQLContainer<?> postgresContainer(Network e2eNetwork) {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:17")
                .withDatabaseName("scrapper")
                .withUsername("user")
                .withPassword("password")
                .withNetwork(e2eNetwork)
                .withNetworkAliases("postgres-db");
        container.start();
        return container;
    }

    @Bean
    public GenericContainer<?> scrapperContainer(Network e2eNetwork, PostgreSQLContainer<?> postgres) {
        LiquibaseMigrationUtil.runMigrations(postgres, "classpath:migrations/master.xml");

        Path jarPath = Paths.get("target").resolve("scrapper-0.0.1.jar");

        GenericContainer<?> container = new GenericContainer<>(
                        new ImageFromDockerfile("localhost/link-tracker-scrapper-e2e:latest", false)
                                .withFileFromPath("app.jar", jarPath)
                                .withDockerfileFromBuilder(builder -> builder.from("eclipse-temurin:25-jre-alpine")
                                        .copy("app.jar", "app.jar")
                                        .expose(8081)
                                        .entryPoint("java", "-jar", "app.jar")
                                        .build()))
                .withNetwork(e2eNetwork)
                .dependsOn(postgres)
                .withExposedPorts(8081)
                .withEnv("DB_URL", "jdbc:postgresql://postgres-db:5432/scrapper")
                .withEnv("DB_USER", postgres.getUsername())
                .withEnv("DB_PASSWORD", postgres.getPassword())
                .withEnv("DB_DRIVER", "org.postgresql.Driver")
                .withEnv("SPRING_LIQUIBASE_ENABLED", "false")
                .withEnv("APP_BOT_URL", "http://stub:8080")
                .withEnv("APP_GRPC_HOST", "stub")
                .withEnv("STACKOVERFLOW_KEY", "test")
                .withEnv("STACKOVERFLOW_ACCESS_KEY", "test")
                .withEnv("GITHUB_TOKEN", "test")
                .withEnv("SERVER_PORT", "8081")
                .waitingFor(Wait.forHttp("/actuator/health").forPort(8081).forStatusCode(200))
                .withStartupTimeout(Duration.ofSeconds(120));

        container.start();
        return container;
    }
}
