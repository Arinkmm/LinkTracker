package backend.academy.linktracker.scrapper.configuration;

import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;

public final class SharedPostgresContainer {
    public static final Network NETWORK = Network.newNetwork();

    public static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>(ContainerConstants.POSTGRES_IMAGE)
            .withDatabaseName(ContainerConstants.DB_NAME)
            .withUsername(ContainerConstants.DB_USER)
            .withPassword(ContainerConstants.DB_PASSWORD)
            .withNetwork(NETWORK)
            .withNetworkAliases(ContainerConstants.DB_NETWORK_ALIAS);

    static {
        INSTANCE.start();
    }

    private SharedPostgresContainer() {}
}
