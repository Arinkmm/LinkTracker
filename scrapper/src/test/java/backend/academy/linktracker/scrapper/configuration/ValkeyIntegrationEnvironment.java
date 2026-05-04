package backend.academy.linktracker.scrapper.configuration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public abstract class ValkeyIntegrationEnvironment extends DatabaseIntegrationEnvironment {
    protected static final GenericContainer<?> valkey = new GenericContainer<>(
                    DockerImageName.parse("valkey/valkey:8.0"))
            .withNetwork(SharedPostgresContainer.NETWORK)
            .withNetworkAliases("valkey-cluster")
            .withExposedPorts(6379)
            .withCommand(
                    "sh",
                    "-c",
                    "valkey-server --cluster-enabled yes --cluster-config-file nodes.conf --cluster-node-timeout 5000 --appendonly yes --bind 0.0.0.0 & "
                            + "sleep 1 && "
                            + "valkey-cli cluster addslots $(seq 0 16383) && "
                            + "wait")
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));

    static {
        valkey.start();
    }

    @DynamicPropertySource
    static void valkeyProperties(DynamicPropertyRegistry registry) {
        String nodes = valkey.getHost() + ":" + valkey.getMappedPort(6379);
        registry.add("spring.data.redis.cluster.nodes", () -> nodes);
        registry.add("spring.cache.redis.time-to-live", () -> "1s");

        registry.add("spring.data.redis.lettuce.cluster.refresh.adaptive", () -> "false");
        registry.add("spring.data.redis.lettuce.cluster.refresh.period", () -> "0");
    }
}
