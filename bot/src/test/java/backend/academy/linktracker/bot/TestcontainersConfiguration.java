package backend.academy.linktracker.bot;

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
        GenericContainer<?> bot = new GenericContainer<>(new ImageFromDockerfile("link-tracker-bot:latest", false)
                        .withDockerfile(Paths.get("./Dockerfile")))
                .withNetwork(network)
                .withExposedPorts(8080)
                .waitingFor(Wait.forHttp("/actuator/health").forPort(8080));

        bot.start();
        return bot;
    }
}
