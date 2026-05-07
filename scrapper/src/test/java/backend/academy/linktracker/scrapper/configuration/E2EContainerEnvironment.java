package backend.academy.linktracker.scrapper.configuration;

import static backend.academy.linktracker.scrapper.configuration.ContainerConstants.*;

import java.nio.file.Paths;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

public final class E2EContainerEnvironment {
    public static final Network NETWORK = Network.newNetwork();

    public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
                    DockerImageName.parse(POSTGRES_IMAGE))
            .withNetwork(NETWORK)
            .withNetworkAliases(DB_NETWORK_ALIAS)
            .withDatabaseName(DB_NAME)
            .withUsername(DB_USER)
            .withPassword(DB_PASSWORD);

    public static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE))
            .withNetwork(NETWORK)
            .withNetworkAliases(KAFKA_ALIAS)
            .withKraft();

    public static final GenericContainer<?> SCHEMA_REGISTRY = new GenericContainer<>(
                    DockerImageName.parse(SCHEMA_REGISTRY_IMAGE))
            .withNetwork(NETWORK)
            .withNetworkAliases(SCHEMA_REGISTRY_ALIAS)
            .dependsOn(KAFKA)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", SCHEMA_REGISTRY_ALIAS)
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://" + KAFKA_ALIAS + ":9092")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:" + SCHEMA_REGISTRY_PORT)
            .withExposedPorts(SCHEMA_REGISTRY_PORT)
            .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));

    public static final GenericContainer<?> WIREMOCK_EXT = new GenericContainer<>(DockerImageName.parse(WIREMOCK_IMAGE))
            .withNetwork(NETWORK)
            .withNetworkAliases(WIREMOCK_EXT_ALIAS)
            .withExposedPorts(WIREMOCK_PORT)
            .waitingFor(Wait.forHttp("/__admin/mappings").forPort(WIREMOCK_PORT).forStatusCode(200));

    public static final GenericContainer<?> WIREMOCK_TG = new GenericContainer<>(DockerImageName.parse(WIREMOCK_IMAGE))
            .withNetwork(NETWORK)
            .withNetworkAliases(WIREMOCK_TG_ALIAS)
            .withCopyToContainer(Transferable.of("""
                            {
                              "request": { "method": "POST", "urlPathPattern": "/bot.*/setMyCommands" },
                              "response": { "status": 200, "jsonBody": { "ok": true, "result": true } }
                            }
                            """), "/home/wiremock/mappings/set_commands.json")
            .withCopyToContainer(Transferable.of("""
                            {
                              "request": { "method": "POST", "urlPathPattern": "/bot.*/sendMessage" },
                              "response": { "status": 200, "jsonBody": { "ok": true, "result": { "message_id": 1 } } }
                            }
                            """), "/home/wiremock/mappings/send_message.json")
            .withExposedPorts(WIREMOCK_PORT)
            .waitingFor(Wait.forHttp("/__admin/mappings").forPort(WIREMOCK_PORT).forStatusCode(200));

    public static final GenericContainer<?> VALKEY = new GenericContainer<>(DockerImageName.parse("valkey/valkey:8.0"))
            .withNetwork(NETWORK)
            .withNetworkAliases("valkey-e2e")
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
        VALKEY.start();
        try {
            VALKEY.execInContainer("sh", "-c", "valkey-cli cluster addslots $(seq 0 16383)");
        } catch (Exception e) {
            throw new RuntimeException("Failed to init E2E Valkey slots");
        }
    }

    public static final GenericContainer<?> SCRAPPER = buildScrapper();
    public static final GenericContainer<?> BOT = buildBot();

    private static GenericContainer<?> buildScrapper() {
        return new GenericContainer<>(new ImageFromDockerfile("localhost/scrapper-e2e:latest", false)
                        .withFileFromPath("app.jar", Paths.get(SCRAPPER_JAR).toAbsolutePath())
                        .withDockerfileFromBuilder(builder -> builder.from("eclipse-temurin:25-jre-alpine")
                                .copy("app.jar", "/app.jar")
                                .expose(SCRAPPER_PORT)
                                .entryPoint("java", "-jar", "/app.jar")
                                .build()))
                .withNetwork(NETWORK)
                .withNetworkAliases(SCRAPPER_ALIAS)
                .dependsOn(POSTGRES, KAFKA, SCHEMA_REGISTRY, WIREMOCK_EXT, VALKEY)
                .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://" + DB_NETWORK_ALIAS + ":5432/" + DB_NAME)
                .withEnv("SPRING_DATASOURCE_USERNAME", DB_USER)
                .withEnv("SPRING_DATASOURCE_PASSWORD", DB_PASSWORD)
                .withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", DB_DRIVER)
                .withEnv("SPRING_LIQUIBASE_ENABLED", "true")
                .withEnv("SCHEDULER_INTERVAL_MS", "5000")
                .withEnv("SPRING_LIQUIBASE_CHANGE_LOG", LIQUIBASE_PATH)
                .withEnv("SPRING_KAFKA_BOOTSTRAP_SERVERS", KAFKA_ALIAS + ":9092")
                .withEnv(
                        "SPRING_KAFKA_PRODUCER_PROPERTIES_SCHEMA_REGISTRY_URL",
                        "http://" + SCHEMA_REGISTRY_ALIAS + ":" + SCHEMA_REGISTRY_PORT)
                .withEnv("APP_GITHUB_URL", "http://" + WIREMOCK_EXT_ALIAS + ":" + WIREMOCK_PORT)
                .withEnv("APP_STACKOVERFLOW_URL", "http://" + WIREMOCK_EXT_ALIAS + ":" + WIREMOCK_PORT)
                .withEnv("APP_KAFKA_TOPIC", TOPIC)
                .withEnv("SPRING_DATA_REDIS_CLUSTER_NODES", "valkey-e2e:6379")
                .withExposedPorts(SCRAPPER_PORT)
                .waitingFor(
                        Wait.forHttp("/actuator/health").forPort(SCRAPPER_PORT).forStatusCode(200))
                .withStartupTimeout(Duration.ofSeconds(120));
    }

    private static GenericContainer<?> buildBot() {
        return new GenericContainer<>(new ImageFromDockerfile("localhost/bot-e2e:latest", false)
                        .withFileFromPath("app.jar", Paths.get(BOT_JAR).toAbsolutePath())
                        .withDockerfileFromBuilder(builder -> builder.from("eclipse-temurin:25-jre-alpine")
                                .copy("app.jar", "/app.jar")
                                .expose(BOT_PORT)
                                .entryPoint("java", "-jar", "/app.jar")
                                .build()))
                .withNetwork(NETWORK)
                .dependsOn(KAFKA, SCHEMA_REGISTRY, WIREMOCK_TG, SCRAPPER)
                .withEnv("SPRING_KAFKA_BOOTSTRAP_SERVERS", KAFKA_ALIAS + ":9092")
                .withEnv(
                        "SPRING_KAFKA_CONSUMER_PROPERTIES_SCHEMA_REGISTRY_URL",
                        "http://" + SCHEMA_REGISTRY_ALIAS + ":" + SCHEMA_REGISTRY_PORT)
                .withEnv("APP_TELEGRAM_URL", "http://" + WIREMOCK_TG_ALIAS + ":" + WIREMOCK_PORT + "/bot")
                .withEnv("APP_SCRAPPER_URL", "http://" + SCRAPPER_ALIAS + ":" + SCRAPPER_PORT)
                .withExposedPorts(BOT_PORT)
                .waitingFor(Wait.forHttp("/actuator/health").forPort(BOT_PORT).forStatusCode(200))
                .withStartupTimeout(Duration.ofSeconds(120));
    }

    private E2EContainerEnvironment() {}
}
