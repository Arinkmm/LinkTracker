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
public class TestcontainersConfiguration {

    @Bean
    public Network containersNetwork() {
        return Network.newNetwork();
    }

    @Bean
    public PostgreSQLContainer<?> postgresContainer(Network containersNetwork) {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:17")
                .withNetwork(containersNetwork)
                .withNetworkAliases("postgres-db")
                .withDatabaseName("scrapper")
                .withUsername("user")
                .withPassword("password");
        container.start();
        return container;
    }

    @Bean
    public GenericContainer<?> scrapperContainer(Network containersNetwork, PostgreSQLContainer<?> postgresContainer) {
        Path jarPath = Paths.get("target").resolve("scrapper-0.0.1.jar");

        return new GenericContainer<>(new ImageFromDockerfile("localhost/link-tracker-scrapper:latest", false)
                        .withFileFromPath("app.jar", jarPath)
                        .withDockerfileFromBuilder(builder -> builder.from("eclipse-temurin:25-jre-alpine")
                                .copy("app.jar", "app.jar")
                                .expose(8081)
                                .entryPoint("java", "-jar", "app.jar")
                                .build()))
                .withNetwork(containersNetwork)
                .dependsOn(postgresContainer)
                .withExposedPorts(8081)
                .withEnv("DB_URL", "jdbc:postgresql://postgres-db:5432/scrapper")
                .withEnv("DB_USER", postgresContainer.getUsername())
                .withEnv("DB_PASSWORD", postgresContainer.getPassword())
                .withEnv("DB_DRIVER", "org.postgresql.Driver")
                .withEnv("SPRING_LIQUIBASE_ENABLED", "false")
                .withEnv("APP_BOT_URL", "http://stub:8080")
                .withEnv("APP_GRPC_HOST", "stub")
                .withEnv("STACKOVERFLOW_KEY", "test")
                .withEnv("STACKOVERFLOW_ACCESS_KEY", "test")
                .withEnv("GITHUB_TOKEN", "test")
                .withEnv("SERVER_PORT", "8081")
                .waitingFor(Wait.forHttp("/actuator/health").forPort(8081).forStatusCode(200))
                .withStartupTimeout(Duration.ofSeconds(90));
    }
}
