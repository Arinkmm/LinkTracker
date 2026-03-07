package backend.academy.linktracker.scrapper;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;


@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    @Bean(name = "customNetwork")
    public Network network() {
        return Network.newNetwork();
    }

    @Bean(name = "scrapperContainer")
    public GenericContainer<?> scrapperContainer(Network network) {
        GenericContainer<?> scrapper = new GenericContainer<>(DockerImageName.parse("link-tracker-scrapper:latest"))
            .withNetwork(network)
            .withExposedPorts(8081)
            .waitingFor(Wait.forHttp("/actuator/health").forPort(8081));

        scrapper.start();
        return scrapper;
    }
}
