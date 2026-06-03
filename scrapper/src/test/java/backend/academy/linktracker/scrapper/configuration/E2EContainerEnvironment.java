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
    private static final String E2E_KAFKA_ALIAS = "kafka-e2e";

    public static final Network NETWORK = SharedPostgresContainer.NETWORK;

    public static final PostgreSQLContainer<?> POSTGRES = SharedPostgresContainer.INSTANCE;

    public static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE))
            .withNetwork(NETWORK)
            .withNetworkAliases(E2E_KAFKA_ALIAS)
            .withKraft();

    public static final GenericContainer<?> SCHEMA_REGISTRY = new GenericContainer<>(
                    DockerImageName.parse(SCHEMA_REGISTRY_IMAGE))
            .withNetwork(NETWORK)
            .withNetworkAliases(SCHEMA_REGISTRY_ALIAS)
            .dependsOn(KAFKA)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", SCHEMA_REGISTRY_ALIAS)
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://" + E2E_KAFKA_ALIAS + ":9092")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:" + SCHEMA_REGISTRY_PORT)
            .withExposedPorts(SCHEMA_REGISTRY_PORT)
            .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));

    public static final GenericContainer<?> WIREMOCK_EXT = new GenericContainer<>(DockerImageName.parse(WIREMOCK_IMAGE))
            .withNetwork(NETWORK)
            .withNetworkAliases(WIREMOCK_EXT_ALIAS)
            .withCopyToContainer(Transferable.of("""
                            {
                              "request": { "method": "POST", "urlPath": "/v1/chat/completions" },
                              "response": {
                                "status": 200,
                                "jsonBody": {
                                  "choices": [
                                    { "message": { "content": "AI summary" } }
                                  ]
                                }
                              }
                            }
                            """), "/home/wiremock/mappings/ai_summary.json")
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
                              "request": { "method": "POST", "urlPathPattern": "/bot.*/getUpdates" },
                              "response": { "status": 200, "jsonBody": { "ok": true, "result": [] } }
                            }
                            """), "/home/wiremock/mappings/get_updates.json")
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
                    "0.0.0.0",
                    "--cluster-announce-ip",
                    "valkey-e2e",
                    "--cluster-announce-port",
                    "6379")
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
    public static final GenericContainer<?> AI_AGENT = buildAiAgent();
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
                .withEnv("SPRING_KAFKA_BOOTSTRAP_SERVERS", E2E_KAFKA_ALIAS + ":9092")
                .withEnv(
                        "SPRING_KAFKA_PRODUCER_PROPERTIES_SCHEMA_REGISTRY_URL",
                        "http://" + SCHEMA_REGISTRY_ALIAS + ":" + SCHEMA_REGISTRY_PORT)
                .withEnv("APP_GITHUB_URL", "http://" + WIREMOCK_EXT_ALIAS + ":" + WIREMOCK_PORT)
                .withEnv("APP_STACKOVERFLOW_URL", "http://" + WIREMOCK_EXT_ALIAS + ":" + WIREMOCK_PORT)
                .withEnv("GITHUB_TOKEN", "test-token")
                .withEnv("STACKOVERFLOW_KEY", "test-key")
                .withEnv("STACKOVERFLOW_ACCESS_KEY", "test-access-token")
                .withEnv("APP_CLIENT_TYPE", "kafka")
                .withEnv("APP_KAFKA_TOPIC", RAW_TOPIC)
                .withEnv("SPRING_DATA_REDIS_CLUSTER_NODES", "valkey-e2e:6379")
                .withExposedPorts(SCRAPPER_PORT)
                .waitingFor(Wait.forHttp("/health").forPort(SCRAPPER_PORT).forStatusCode(200))
                .withStartupTimeout(Duration.ofSeconds(120));
    }

    private static GenericContainer<?> buildAiAgent() {
        return new GenericContainer<>(new ImageFromDockerfile("localhost/ai-agent-e2e:latest", false)
                        .withFileFromPath("app.jar", Paths.get(AI_AGENT_JAR).toAbsolutePath())
                        .withDockerfileFromBuilder(builder -> builder.from("eclipse-temurin:25-jre-alpine")
                                .copy("app.jar", "/app.jar")
                                .expose(AI_AGENT_PORT)
                                .entryPoint("java", "-jar", "/app.jar")
                                .build()))
                .withNetwork(NETWORK)
                .withNetworkAliases(AI_AGENT_ALIAS)
                .dependsOn(KAFKA, SCHEMA_REGISTRY, WIREMOCK_EXT)
                .withEnv("SPRING_KAFKA_BOOTSTRAP_SERVERS", E2E_KAFKA_ALIAS + ":9092")
                .withEnv(
                        "SPRING_KAFKA_CONSUMER_PROPERTIES_SCHEMA_REGISTRY_URL",
                        "http://" + SCHEMA_REGISTRY_ALIAS + ":" + SCHEMA_REGISTRY_PORT)
                .withEnv(
                        "SPRING_KAFKA_PRODUCER_PROPERTIES_SCHEMA_REGISTRY_URL",
                        "http://" + SCHEMA_REGISTRY_ALIAS + ":" + SCHEMA_REGISTRY_PORT)
                .withEnv("APP_KAFKA_RAW_TOPIC", RAW_TOPIC)
                .withEnv("APP_KAFKA_PROCESSED_TOPIC", PROCESSED_TOPIC)
                .withEnv("AI_AGENT_SUMMARIZATION_THRESHOLD", "10000")
                .withEnv("AI_AGENT_GROUPING_WINDOW_MS", "1000")
                .withEnv("AI_AGENT_GROUPING_FLUSH_INTERVAL_MS", "500")
                .withEnv(
                        "AI_AGENT_SUMMARIZATION_API_URL",
                        "http://" + WIREMOCK_EXT_ALIAS + ":" + WIREMOCK_PORT + "/v1/chat/completions")
                .withEnv("AI_AGENT_SUMMARIZATION_API_TOKEN", "test-token")
                .withExposedPorts(AI_AGENT_PORT)
                .waitingFor(
                        Wait.forHttp("/actuator/health").forPort(AI_AGENT_PORT).forStatusCode(200))
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
                .dependsOn(KAFKA, SCHEMA_REGISTRY, WIREMOCK_TG, SCRAPPER, AI_AGENT)
                .withEnv("SPRING_KAFKA_BOOTSTRAP_SERVERS", E2E_KAFKA_ALIAS + ":9092")
                .withEnv(
                        "SPRING_KAFKA_CONSUMER_PROPERTIES_SCHEMA_REGISTRY_URL",
                        "http://" + SCHEMA_REGISTRY_ALIAS + ":" + SCHEMA_REGISTRY_PORT)
                .withEnv("APP_KAFKA_TOPIC", PROCESSED_TOPIC)
                .withEnv("APP_TELEGRAM_URL", "http://" + WIREMOCK_TG_ALIAS + ":" + WIREMOCK_PORT + "/bot")
                .withEnv("APP_UPDATES_TYPE", "kafka")
                .withEnv("TELEGRAM_TOKEN", "test-token")
                .withEnv("APP_SCRAPPER_URL", "http://" + SCRAPPER_ALIAS + ":" + SCRAPPER_PORT)
                .withExposedPorts(BOT_PORT, 8011)
                .waitingFor(Wait.forHttp("/health").forPort(8011).forStatusCode(200))
                .withStartupTimeout(Duration.ofSeconds(120));
    }

    private E2EContainerEnvironment() {}
}
