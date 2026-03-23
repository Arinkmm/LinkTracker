package backend.academy.linktracker.bot.integration;

import static org.junit.jupiter.api.Assertions.*;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class BotIntegrationContainerTest {
    @Autowired
    @Qualifier("botContainer")
    private GenericContainer<?> botContainer;

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
