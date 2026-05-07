package backend.academy.linktracker.scrapper.configuration;

import static backend.academy.linktracker.scrapper.configuration.SharedPostgresContainer.NETWORK;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public abstract class CacheIntegrationEnvironment extends DatabaseIntegrationEnvironment {
    protected static final GenericContainer<?> valkey = new GenericContainer<>(
                    DockerImageName.parse("valkey/valkey:8.0"))
            .withNetwork(NETWORK)
            .withNetworkAliases("valkey-cluster")
            .withExposedPorts(6379)
            .withCommand(
                    "valkey-server",
                    "--cluster-enabled",
                    "yes",
                    "--cluster-config-file",
                    "nodes.conf",
                    "--appendonly",
                    "yes",
                    "--bind",
                    "0.0.0.0")
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));

    static {
        valkey.start();
        try {
            valkey.execInContainer("sh", "-c", "valkey-cli cluster addslots $(seq 0 16383)");

            String host = valkey.getHost();
            int port = valkey.getMappedPort(6379);
            valkey.execInContainer("valkey-cli", "config", "set", "cluster-announce-ip", host);
            valkey.execInContainer("valkey-cli", "config", "set", "cluster-announce-port", String.valueOf(port));
        } catch (Exception e) {
            throw new RuntimeException("Failed to init E2E Valkey slots");
        }
    }

    @DynamicPropertySource
    static void valkeyProperties(DynamicPropertyRegistry registry) {
        String nodes = valkey.getHost() + ":" + valkey.getMappedPort(6379);
        registry.add("spring.data.redis.cluster.nodes", () -> nodes);
        registry.add("spring.data.redis.lettuce.cluster.refresh.adaptive", () -> "false");
        registry.add("app.cache.l2-ttl", () -> "1s");
        registry.add("app.cache.l1-ttl", () -> "500ms");
        registry.add("app.cache.l1-capacity", () -> "100");
    }
}
