package backend.academy.linktracker.scrapper;

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
        GenericContainer<?> scrapper = new GenericContainer<>(
                        new ImageFromDockerfile("link-tracker-scrapper:latest", false)
                                .withDockerfile(Paths.get("./Dockerfile")))
                .withNetwork(network)
                .withExposedPorts(8081)
                .waitingFor(Wait.forHttp("/actuator/health").forPort(8081));

        scrapper.start();
        return scrapper;
    }
}
