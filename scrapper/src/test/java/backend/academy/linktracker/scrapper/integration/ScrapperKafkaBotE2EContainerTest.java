package backend.academy.linktracker.scrapper.integration;

import static backend.academy.linktracker.scrapper.configuration.ContainerConstants.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class ScrapperKafkaBotE2EContainerTest {
    private static final Network NETWORK = Network.newNetwork();
    private static final AtomicLong chatIdGen = new AtomicLong(5_000L);

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
        .withNetwork(NETWORK)
        .withNetworkAliases(DB_NETWORK_ALIAS)
        .withDatabaseName(DB_NAME)
        .withUsername(DB_USER)
        .withPassword(DB_PASSWORD);

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE))
        .withNetwork(NETWORK)
        .withNetworkAliases(KAFKA_ALIAS);

    @Container
    static final GenericContainer<?> schemaRegistry = new GenericContainer<>(DockerImageName.parse(SCHEMA_REGISTRY_IMAGE))
        .withNetwork(NETWORK)
        .withNetworkAliases(SCHEMA_REGISTRY_ALIAS)
        .dependsOn(kafka)
        .withEnv("SCHEMA_REGISTRY_HOST_NAME", SCHEMA_REGISTRY_ALIAS)
        .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://" + KAFKA_ALIAS + ":9092")
        .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:" + SCHEMA_REGISTRY_PORT)
        .withExposedPorts(SCHEMA_REGISTRY_PORT)
        .waitingFor(Wait.forHttp("/subjects").forStatusCode(200));

    @Container
    static final GenericContainer<?> wireMockExt = new GenericContainer<>(DockerImageName.parse(WIREMOCK_IMAGE))
        .withNetwork(NETWORK)
        .withNetworkAliases(WIREMOCK_EXT_ALIAS)
        .withExposedPorts(WIREMOCK_PORT)
        .waitingFor(Wait.forHttp("/__admin/mappings").forPort(WIREMOCK_PORT).forStatusCode(200));

    @Container
    static final GenericContainer<?> wireMockTg = new GenericContainer<>(DockerImageName.parse(WIREMOCK_IMAGE))
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

    @Container
    static final GenericContainer<?> scrapper = createScrapperContainer();

    @Container
    static final GenericContainer<?> bot = createBotContainer();

    private static GenericContainer<?> createScrapperContainer() {
        return new GenericContainer<>(
            new ImageFromDockerfile("localhost/scrapper-e2e:latest", false)
                .withFileFromPath("app.jar", Paths.get(SCRAPPER_JAR))
                .withDockerfileFromBuilder(builder -> builder
                    .from("eclipse-temurin:25-jre-alpine")
                    .copy("app.jar", "/app.jar")
                    .expose(SCRAPPER_PORT)
                    .entryPoint("java", "-jar", "/app.jar")
                    .build()))
            .withNetwork(NETWORK)
            .withNetworkAliases(SCRAPPER_ALIAS)
            .dependsOn(postgres, kafka, schemaRegistry, wireMockExt)
            .withEnv("SPRING_DATASOURCE_URL", "jdbc:postgresql://" + DB_NETWORK_ALIAS + ":5432/" + DB_NAME)
            .withEnv("SPRING_DATASOURCE_USERNAME", DB_USER)
            .withEnv("SPRING_DATASOURCE_PASSWORD", DB_PASSWORD)
            .withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", DB_DRIVER)
            .withEnv("SPRING_LIQUIBASE_CHANGE_LOG", LIQUIBASE_PATH)
            .withEnv("SPRING_KAFKA_BOOTSTRAP_SERVERS", KAFKA_ALIAS + ":9092")
            .withEnv("SPRING_KAFKA_PRODUCER_PROPERTIES_SCHEMA_REGISTRY_URL", "http://" + SCHEMA_REGISTRY_ALIAS + ":" + SCHEMA_REGISTRY_PORT)
            .withEnv("APP_GITHUB_URL", "http://" + WIREMOCK_EXT_ALIAS + ":" + WIREMOCK_PORT)
            .withEnv("APP_KAFKA_TOPIC", TOPIC)
            .withExposedPorts(SCRAPPER_PORT)
            .waitingFor(Wait.forHttp("/actuator/health").forPort(SCRAPPER_PORT).forStatusCode(200));
    }

    private static GenericContainer<?> createBotContainer() {
        return new GenericContainer<>(
            new ImageFromDockerfile("localhost/bot-e2e:latest", false)
                .withFileFromPath("app.jar", Paths.get(BOT_JAR))
                .withDockerfileFromBuilder(builder -> builder
                    .from("eclipse-temurin:25-jre-alpine")
                    .copy("app.jar", "/app.jar")
                    .expose(BOT_PORT)
                    .entryPoint("java", "-jar", "/app.jar")
                    .build()))
            .withNetwork(NETWORK)
            .dependsOn(kafka, schemaRegistry, wireMockTg)
            .withEnv("SPRING_KAFKA_BOOTSTRAP_SERVERS", KAFKA_ALIAS + ":9092")
            .withEnv("SPRING_KAFKA_CONSUMER_PROPERTIES_SCHEMA_REGISTRY_URL", "http://" + SCHEMA_REGISTRY_ALIAS + ":" + SCHEMA_REGISTRY_PORT)
            .withEnv("APP_TELEGRAM_URL", "http://" + WIREMOCK_TG_ALIAS + ":" + WIREMOCK_PORT + "/bot")
            .withEnv("APP_SCRAPPER_URL", "http://" + SCRAPPER_ALIAS + ":" + SCRAPPER_PORT)
            .withExposedPorts(BOT_PORT)
            .waitingFor(Wait.forHttp("/actuator/health").forPort(BOT_PORT).forStatusCode(200));
    }

    private WireMock extWireMock;
    private WireMock tgWireMock;

    @BeforeEach
    void initWireMockClients() {
        extWireMock = new WireMock(wireMockExt.getHost(), wireMockExt.getMappedPort(WIREMOCK_PORT));
        tgWireMock = new WireMock(wireMockTg.getHost(), wireMockTg.getMappedPort(WIREMOCK_PORT));
        extWireMock.resetMappings();
        tgWireMock.resetRequests();
    }

    @Test
    @DisplayName("E2E: Scrapper -> Kafka -> Bot -> Telegram")
    void shouldDeliverGithubUpdateFromScrapperViаKafkaToBot() {
        long chatId = chatIdGen.getAndIncrement();
        String owner = "test-user";
        String repo = "test-repo";
        String url = "https://github.com/" + owner + "/" + repo;

        stubGitHub(owner, repo, Instant.now().plus(1, ChronoUnit.DAYS).toString());

        RestClient scrClient = RestClient.builder()
            .baseUrl("http://" + scrapper.getHost() + ":" + scrapper.getMappedPort(SCRAPPER_PORT))
            .build();

        assertDoesNotThrow(() -> {
            scrClient.post().uri("/tg-chat/{id}", chatId).retrieve().toBodilessEntity();

            AddLinkRequest req = new AddLinkRequest();
            req.setLink(URI.create(url));
            scrClient.post().uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(req).retrieve().toBodilessEntity();
        });

        Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofSeconds(2))
            .untilAsserted(() ->
                extWireMock.verifyThat(getRequestedFor(urlPathMatching("/repos/.*")))
            );

        try {
            Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() ->
                    tgWireMock.verifyThat(
                        postRequestedFor(urlPathMatching("/bot.*/sendMessage"))
                            .withRequestBody(containing("chat_id=" + chatId))
                    )
                );
        } catch (Throwable t) {
            System.err.println("--- FAILURE DIAGNOSTICS ---");
            System.err.println("SCRAPPER LOGS:\n" + scrapper.getLogs());
            System.err.println("BOT LOGS:\n" + bot.getLogs());
            throw t;
        }
    }

    private void stubGitHub(String owner, String repo, String createdAt) {
        extWireMock.register(
            WireMock.get(WireMock.urlPathMatching("/repos/" + owner + "/" + repo + "/issues"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("[{\"id\":1, \"created_at\":\"" + createdAt + "\", \"state\":\"open\"}]")));
    }
}
