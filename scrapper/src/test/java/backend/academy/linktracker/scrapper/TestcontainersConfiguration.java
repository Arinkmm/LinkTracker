package backend.academy.linktracker.scrapper;

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

    @Bean(name = "scrapperContainer")
    public GenericContainer<?> scrapperContainer(Network network) {
        Path jarPath = Paths.get("target/scrapper-0.0.1.jar");

        return new GenericContainer<>(new ImageFromDockerfile("link-tracker-scrapper:latest", false)
                        .withFileFromPath("Dockerfile", Paths.get("Dockerfile"))
                        .withFileFromPath("target/scrapper-0.0.1.jar", jarPath))
                .withNetwork(network)
                .withExposedPorts(8081)
                .waitingFor(Wait.forHttp("/actuator/health").forPort(8081));
    }
}
