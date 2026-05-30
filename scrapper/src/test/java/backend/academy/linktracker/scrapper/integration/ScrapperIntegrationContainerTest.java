package backend.academy.linktracker.scrapper.integration;

import static org.junit.jupiter.api.Assertions.*;

import backend.academy.linktracker.scrapper.configuration.ContainerConstants;
import backend.academy.linktracker.scrapper.configuration.SharedPostgresContainer;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class ScrapperIntegrationContainerTest {
    static final Network NETWORK = SharedPostgresContainer.NETWORK;

    static final GenericContainer<?> valkey = new GenericContainer<>(DockerImageName.parse("valkey/valkey:8.0"))
            .withNetwork(NETWORK)
            .withNetworkAliases("valkey")
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
                    "valkey",
                    "--cluster-announce-port",
                    "6379")
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));

    static {
        valkey.start();
        try {
            valkey.execInContainer("sh", "-c", "valkey-cli cluster addslots $(seq 0 16383)");
        } catch (Exception e) {
            throw new RuntimeException("Failed to init Valkey slots", e);
        }
    }

    @Container
    static final GenericContainer<?> scrapper = new GenericContainer<>(
                    new ImageFromDockerfile(ContainerConstants.SCRAPPER_IMAGE, false)
                            .withFileFromPath("app.jar", Paths.get(ContainerConstants.SCRAPPER_JAR))
                            .withDockerfileFromBuilder(builder -> builder.from("eclipse-temurin:25-jre-alpine")
                                    .copy("app.jar", "app.jar")
                                    .expose(ContainerConstants.SCRAPPER_PORT)
                                    .entryPoint("java", "-Dspring.profiles.active=test", "-jar", "app.jar")
                                    .build()))
            .withNetwork(SharedPostgresContainer.NETWORK)
            .dependsOn(SharedPostgresContainer.INSTANCE, valkey)
            .withExposedPorts(ContainerConstants.SCRAPPER_PORT)
            .withEnv(
                    "DB_URL",
                    "jdbc:postgresql://" + ContainerConstants.DB_NETWORK_ALIAS + ":5432/" + ContainerConstants.DB_NAME)
            .withEnv("DB_USER", ContainerConstants.DB_USER)
            .withEnv("DB_PASSWORD", ContainerConstants.DB_PASSWORD)
            .withEnv("DB_DRIVER", ContainerConstants.DB_DRIVER)
            .withEnv("SPRING_LIQUIBASE_ENABLED", "true")
            .withEnv("SPRING_LIQUIBASE_CHANGE_LOG", ContainerConstants.LIQUIBASE_PATH)
            .withEnv("REDIS_CLUSTER_NODES", "valkey:6379")
            .withEnv("GITHUB_TOKEN", "test-token")
            .withEnv("STACKOVERFLOW_KEY", "test-key")
            .withEnv("STACKOVERFLOW_ACCESS_KEY", "test-access-token")
            .waitingFor(Wait.forHttp("/health")
                    .forPort(ContainerConstants.SCRAPPER_PORT)
                    .forStatusCode(200))
            .withStartupTimeout(Duration.ofSeconds(120));

    private static final AtomicLong chatIdCounter = new AtomicLong(100L);

    private RestClient restClient() {
        String host = scrapper.getHost();
        Integer port = scrapper.getMappedPort(8081);
        return RestClient.builder().baseUrl("http://" + host + ":" + port).build();
    }

    private AddLinkRequest linkRequest(String url) {
        AddLinkRequest req = new AddLinkRequest();
        req.setLink(URI.create(url));
        req.setTags(List.of());
        req.setFilters(List.of());
        return req;
    }

    private void registerChat(long chatId) {
        restClient()
                .method(HttpMethod.POST)
                .uri("/tg-chat/{id}", chatId)
                .retrieve()
                .toBodilessEntity();
    }

    private void addLink(long chatId, String url) {
        restClient()
                .method(HttpMethod.POST)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(linkRequest(url))
                .retrieve()
                .toBodilessEntity();
    }

    @Test
    @DisplayName("Тест 3.1: Добавление и получение ссылки")
    void test3_1_addAndGetLink() {
        long chatId = chatIdCounter.getAndIncrement();
        String link = "https://github.com/user/repo";

        registerChat(chatId);
        addLink(chatId, link);

        String response = restClient()
                .method(HttpMethod.GET)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .retrieve()
                .body(String.class);

        assertNotNull(response);
        assertTrue(response.contains(link));
    }

    @Test
    @DisplayName("Тест 3.2: Добавление и удаление ссылки")
    void test3_2_addAndDeleteLink() {
        long chatId = chatIdCounter.getAndIncrement();
        String link = "https://google.com";

        registerChat(chatId);
        addLink(chatId, link);

        restClient()
                .method(HttpMethod.DELETE)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(linkRequest(link))
                .retrieve()
                .toBodilessEntity();

        String response = restClient()
                .method(HttpMethod.GET)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .retrieve()
                .body(String.class);

        assertNotNull(response);
        assertFalse(response.contains(link));
    }

    @Test
    @DisplayName("Тест 3.3: Удаление ссылки из несуществующего чата")
    void test3_3_deleteFromMissingChat() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient()
                .method(HttpMethod.DELETE)
                .uri("/links")
                .header("Tg-Chat-Id", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .body(linkRequest("https://yandex.ru"))
                .retrieve()
                .toBodilessEntity());

        assertTrue(ex.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Тест 3.4: Добавление ссылки в несуществующий чат")
    void test3_4_addToMissingChat() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient()
                .method(HttpMethod.POST)
                .uri("/links")
                .header("Tg-Chat-Id", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .body(linkRequest("https://github.com"))
                .retrieve()
                .toBodilessEntity());

        assertTrue(ex.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Тест 3.5: Работа с удалённым чатом")
    void test3_5_workWithDeletedChat() {
        long chatId = chatIdCounter.getAndIncrement();

        registerChat(chatId);

        restClient()
                .method(HttpMethod.DELETE)
                .uri("/tg-chat/{id}", chatId)
                .retrieve()
                .toBodilessEntity();

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient()
                .method(HttpMethod.POST)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(linkRequest("https://github.com"))
                .retrieve()
                .toBodilessEntity());

        assertTrue(ex.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Тест 3.6: Удаление несуществующего чата")
    void test3_6_deleteMissingChat() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient()
                .method(HttpMethod.DELETE)
                .uri("/tg-chat/100500")
                .retrieve()
                .toBodilessEntity());

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
