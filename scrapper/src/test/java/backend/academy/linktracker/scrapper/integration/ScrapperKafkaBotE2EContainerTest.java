package backend.academy.linktracker.scrapper.integration;

import static backend.academy.linktracker.scrapper.configuration.ContainerConstants.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import backend.academy.linktracker.scrapper.configuration.E2EContainerEnvironment;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.URI;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ScrapperKafkaBotE2EContainerTest {
    private static final AtomicLong chatIdGen = new AtomicLong(5_000L);

    @Container
    static final PostgreSQLContainer<?> postgres = E2EContainerEnvironment.POSTGRES;

    @Container
    static final KafkaContainer kafka = E2EContainerEnvironment.KAFKA;

    @Container
    static final GenericContainer<?> schemaRegistry = E2EContainerEnvironment.SCHEMA_REGISTRY;

    static final GenericContainer<?> valkey = E2EContainerEnvironment.VALKEY;

    @Container
    static final GenericContainer<?> wireMockExt = E2EContainerEnvironment.WIREMOCK_EXT;

    @Container
    static final GenericContainer<?> wireMockTg = E2EContainerEnvironment.WIREMOCK_TG;

    @Container
    static final GenericContainer<?> scrapper = E2EContainerEnvironment.SCRAPPER;

    @Container
    static final GenericContainer<?> aiAgent = E2EContainerEnvironment.AI_AGENT;

    @Container
    static final GenericContainer<?> bot = E2EContainerEnvironment.BOT;

    private WireMock extWireMock;
    private WireMock tgWireMock;

    @BeforeEach
    void initWireMockClients() {
        extWireMock = new WireMock(wireMockExt.getHost(), wireMockExt.getMappedPort(WIREMOCK_PORT));
        tgWireMock = new WireMock(wireMockTg.getHost(), wireMockTg.getMappedPort(WIREMOCK_PORT));

        extWireMock.resetRequests();
        tgWireMock.resetRequests();
    }

    @Test
    @DisplayName("E2E: Scrapper отправляет обновление в Bot через Kafka")
    void shouldDeliverGithubUpdateFromScrapperViaKafkaToBot() {
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
            scrClient
                    .post()
                    .uri("/links")
                    .header("Tg-Chat-Id", String.valueOf(chatId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .toBodilessEntity();
        });

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> extWireMock.verifyThat(getRequestedFor(urlPathMatching("/repos/.*"))));

        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> tgWireMock.verifyThat(postRequestedFor(urlPathMatching("/bot.*/sendMessage"))
                        .withRequestBody(containing("chat_id=" + chatId))));
    }

    private void stubGitHub(String owner, String repo, String createdAt) {
        extWireMock.register(WireMock.get(WireMock.urlPathMatching("/repos/" + owner + "/" + repo + "/issues"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":1, \"created_at\":\"" + createdAt + "\", \"state\":\"open\"}]")));
    }
}
