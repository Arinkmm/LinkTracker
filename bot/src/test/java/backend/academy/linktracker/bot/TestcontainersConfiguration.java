package backend.academy.linktracker.bot;

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

    @Bean(name = "botContainer")
    public GenericContainer<?> botContainer(Network network) {
        GenericContainer<?> bot = new GenericContainer<>(DockerImageName.parse("link-tracker-bot:latest"))
            .withNetwork(network)
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/actuator/health").forPort(8080));

        bot.start();
        return bot;
    }
}
