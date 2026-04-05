package backend.academy.linktracker.bot.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class BotIntegrationContainerTest {
    private static final int BOT_PORT = 8080;
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(180);

    @Container
    static final GenericContainer<?> botContainer = new GenericContainer<>(
                    new ImageFromDockerfile("localhost/link-tracker-bot-test:latest", false)
                            .withFileFromPath(".", findProjectRoot())
                            .withDockerfile(findProjectRoot().resolve("Dockerfile")))
            .withExposedPorts(BOT_PORT)
            .withStartupTimeout(STARTUP_TIMEOUT)
            .waitingFor(Wait.forHttp("/actuator/health")
                    .forPort(BOT_PORT)
                    .forStatusCode(200)
                    .withStartupTimeout(STARTUP_TIMEOUT));

    private RestClient restClient;

    @BeforeEach
    void setup() {
        restClient = RestClient.builder()
                .baseUrl("http://" + botContainer.getHost() + ":" + botContainer.getMappedPort(BOT_PORT))
                .build();
    }

    @Test
    @DisplayName("Тест 1: Корректный запрос к сервису Бота")
    void test1_correctRequest() {
        LinkUpdate update = new LinkUpdate();
        update.setId(1L);
        update.setUrl(URI.create("https://github.com/spring"));
        update.setDescription("New update");
        update.setTgChatIds(List.of(1L, 2L));

        ResponseEntity<Void> response = restClient
                .method(HttpMethod.POST)
                .uri("/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .body(update)
                .retrieve()
                .toBodilessEntity();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Тест 2: Некорректный запрос к сервису Бота")
    void test2_incorrectRequest() {
        String invalidBody = """
            {
              "id": "not-a-number",
              "url": ""
            }
            """;

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient
                .method(HttpMethod.POST)
                .uri("/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidBody)
                .retrieve()
                .toBodilessEntity());

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    private static Path findProjectRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();

        while (current != null) {
            boolean hasDockerfile = Files.exists(current.resolve("Dockerfile"));
            boolean hasRootPom = Files.exists(current.resolve("pom.xml"));
            boolean hasBotModule = Files.exists(current.resolve("bot").resolve("pom.xml"));
            boolean hasApiCommonModule =
                    Files.exists(current.resolve("api-common").resolve("pom.xml"));

            if (hasDockerfile && hasRootPom && hasBotModule && hasApiCommonModule) {
                return current;
            }

            current = current.getParent();
        }

        throw new IllegalStateException("Не удалось найти корень проекта с Dockerfile и multi-module Maven structure");
    }
}
