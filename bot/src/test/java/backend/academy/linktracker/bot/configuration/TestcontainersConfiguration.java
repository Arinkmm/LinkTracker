package backend.academy.linktracker.bot.configuration;

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
    @Bean(name = "customNetwork")
    public Network network() {
        return Network.newNetwork();
    }

    @Bean(name = "botContainer")
    public GenericContainer<?> botContainer(Network network) {
        String jarName = "bot-0.0.1.jar";
        Path jarPath = Paths.get("target").resolve(jarName);

        return new GenericContainer<>(new ImageFromDockerfile("localhost/link-tracker-bot:latest", false)
                        .withFileFromPath("app.jar", jarPath)
                        .withDockerfileFromBuilder(builder -> builder.from("eclipse-temurin:25-jre-alpine")
                                .copy("app.jar", "app.jar")
                                .expose(8081)
                                .entryPoint("java", "-jar", "app.jar")
                                .build()))
                .withNetwork(network)
                .withExposedPorts(8080)
                .withStartupTimeout(Duration.ofSeconds(120))
                .waitingFor(Wait.forHttp("/actuator/health").forPort(8080));
    }
}
