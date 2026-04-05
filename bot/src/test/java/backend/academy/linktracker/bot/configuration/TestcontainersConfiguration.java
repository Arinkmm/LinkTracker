package backend.academy.linktracker.bot.configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
    private static final String BOT_JAR_NAME = "bot-0.0.1.jar";
    private static final int BOT_PORT = 8080;

    @Bean(name = "customNetwork", destroyMethod = "close")
    public Network network() {
        return Network.newNetwork();
    }

    @Bean(name = "botContainer", initMethod = "start", destroyMethod = "stop")
    public GenericContainer<?> botContainer(Network network) {
        Path jarPath = resolveBotJarPath();

        return new GenericContainer<>(new ImageFromDockerfile("localhost/link-tracker-bot:latest", false)
                        .withFileFromPath("app.jar", jarPath)
                        .withDockerfileFromBuilder(builder -> builder.from("eclipse-temurin:25-jre-alpine")
                                .copy("app.jar", "/app.jar")
                                .expose(BOT_PORT)
                                .entryPoint("java", "-jar", "/app.jar")
                                .build()))
                .withNetwork(network)
                .withExposedPorts(BOT_PORT)
                .withStartupTimeout(Duration.ofSeconds(120))
                .waitingFor(Wait.forHttp("/actuator/health")
                        .forPort(BOT_PORT)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofSeconds(120)));
    }

    private Path resolveBotJarPath() {
        Path[] candidates = {Paths.get("target", BOT_JAR_NAME), Paths.get("bot", "target", BOT_JAR_NAME)};

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.toAbsolutePath();
            }
        }

        throw new IllegalStateException("Не найден jar для bot container. Ожидался один из путей: "
                + candidates[0].toAbsolutePath() + " или "
                + candidates[1].toAbsolutePath());
    }
}
