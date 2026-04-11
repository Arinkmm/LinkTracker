package backend.academy.linktracker.bot.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@Testcontainers
class BotIntegrationContainerTest {
    private static final Network NETWORK = Network.newNetwork();

    @Container
    static final GenericContainer<?> wiremockContainer = new GenericContainer<>("wiremock/wiremock:3.13.1")
            .withNetwork(NETWORK)
            .withNetworkAliases("wiremock")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("mappings/telegram-setmycommands.json"),
                    "/home/wiremock/mappings/telegram-setmycommands.json")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("mappings/telegram-sendmessage.json"),
                    "/home/wiremock/mappings/telegram-sendmessage.json")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/__admin/mappings").forPort(8080).forStatusCode(200))
            .withStartupTimeout(Duration.ofSeconds(60));

    @Container
    static final GenericContainer<?> botContainer = createBotContainer();

    private static GenericContainer<?> createBotContainer() {
        String jarName = "bot-0.0.1.jar";
        Path jarPath = Paths.get("target").resolve(jarName);

        return new GenericContainer<>(new ImageFromDockerfile("localhost/link-tracker-bot:latest", false)
                        .withFileFromPath("app.jar", jarPath)
                        .withDockerfileFromBuilder(builder -> builder.from("eclipse-temurin:25-jre-alpine")
                                .copy("app.jar", "/app.jar")
                                .expose(8080)
                                .entryPoint("java", "-jar", "/app.jar")
                                .build()))
                .dependsOn(wiremockContainer)
                .withNetwork(NETWORK)
                .withExposedPorts(8080)
                .withEnv("APP_TELEGRAM_URL", "http://wiremock:8080/bot")
                .withEnv("APP_TELEGRAM_TOKEN", "test-token")
                .waitingFor(Wait.forHttp("/actuator/health").forPort(8080).forStatusCode(200))
                .withStartupTimeout(Duration.ofSeconds(120));
    }

    private RestClient restClient() {
        String host = botContainer.getHost();
        Integer port = botContainer.getMappedPort(8080);
        return RestClient.builder().baseUrl("http://" + host + ":" + port).build();
    }

    @Test
    @DisplayName("Тест 1: Корректный запрос к сервису Бота")
    void test1_correctRequest() {
        LinkUpdate update = new LinkUpdate();
        update.setId(1L);
        update.setUrl(URI.create("https://github.com/spring"));
        update.setDescription("New update");
        update.setTgChatIds(List.of(1L, 2L));

        ResponseEntity<Void> response = restClient()
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

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            restClient()
                    .method(HttpMethod.POST)
                    .uri("/updates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(invalidBody)
                    .retrieve()
                    .toBodilessEntity();
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
