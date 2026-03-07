package backend.academy.linktracker.bot;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    @Bean(name = "customNetwork")
    public Network network() {
        return Network.newNetwork();
    }

    @Bean(name = "botContainer")
    public GenericContainer<?> botContainer(Network network) {
        Path jarPath = Paths.get("target/bot-0.0.1.jar");

        return new GenericContainer<>(new ImageFromDockerfile("localhost/link-tracker-bot:latest", false)
                        .withFileFromPath("Dockerfile", Paths.get("Dockerfile"))
                        .withFileFromPath("target/bot-0.0.1.jar", jarPath))
                .withNetwork(network)
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/actuator/health").forPort(8080));
    }
}
