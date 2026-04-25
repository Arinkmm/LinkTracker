package backend.academy.linktracker.scrapper.configuration;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public final class SharedKafkaContainer {
    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:7.6.0";

    public static final KafkaContainer INSTANCE = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE))
            .withNetwork(SharedPostgresContainer.NETWORK)
            .withNetworkAliases("kafka");

    static {
        INSTANCE.start();
    }

    private SharedKafkaContainer() {}
}
