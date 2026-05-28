package backend.academy.linktracker.bot.configuration;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public final class SharedKafkaContainer {
    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:8.2.0";

    public static final KafkaContainer INSTANCE = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE)).withKraft();

    static {
        INSTANCE.start();
    }

    private SharedKafkaContainer() {}
}
