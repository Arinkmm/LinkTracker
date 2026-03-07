package backend.academy.linktracker.scrapper;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
class ScrapperIntegrationContainerTest {
    @Autowired
    @Qualifier("scrapperContainer")
    private GenericContainer<?> scrapperContainer;

    private RestClient restClient() {
        String host = scrapperContainer.getHost();
        Integer port = scrapperContainer.getMappedPort(8081);
        return RestClient.builder()
            .baseUrl("http://" + host + ":" + port)
            .build();
    }

    @Test
    @DisplayName("Тест 3.1: Добавление и получение ссылки")
    void test3_1_addAndGetLink() {
        long chatId = 1L;
        String link = "https://github.com/user/repo";

        restClient().method(HttpMethod.POST).uri("/tg-chat/{id}", chatId).retrieve().toBodilessEntity();

        restClient().method(HttpMethod.POST).uri("/links")
            .header("Tg-Chat-Id", String.valueOf(chatId))
            .contentType(MediaType.APPLICATION_JSON)
            .body(new AddLinkRequest(link, null, null))
            .retrieve().toBodilessEntity();

        String response = restClient().method(HttpMethod.GET).uri("/links")
            .header("Tg-Chat-Id", String.valueOf(chatId))
            .retrieve().body(String.class);

        assertNotNull(response);
        assertTrue(response.contains(link));
    }

    @Test
    @DisplayName("Тест 3.2: Добавление и удаление ссылки")
    void test3_2_addAndDeleteLink() {
        long chatId = 10L;
        String link = "https://google.com";

        restClient().method(HttpMethod.POST).uri("/tg-chat/{id}", chatId).retrieve().toBodilessEntity();

        restClient().method(HttpMethod.POST).uri("/links")
            .header("Tg-Chat-Id", String.valueOf(chatId))
            .contentType(MediaType.APPLICATION_JSON)
            .body(new AddLinkRequest(link, null, null))
            .retrieve().toBodilessEntity();

        restClient().method(HttpMethod.DELETE).uri("/links")
            .header("Tg-Chat-Id", String.valueOf(chatId))
            .contentType(MediaType.APPLICATION_JSON)
            .body(new AddLinkRequest(link, null, null))
            .retrieve().toBodilessEntity();

        String response = restClient().method(HttpMethod.GET).uri("/links")
            .header("Tg-Chat-Id", String.valueOf(chatId))
            .retrieve().body(String.class);

        assertTrue(response != null && !response.contains(link));
    }

    @Test
    @DisplayName("Тест 3.3: Удаление ссылки из несуществующего чата")
    void test3_3_deleteFromMissingChat() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            restClient().method(HttpMethod.DELETE).uri("/links")
                .header("Tg-Chat-Id", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddLinkRequest("https://yandex.ru", null, null))
                .retrieve().toBodilessEntity();
        });
        assertTrue(ex.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Тест 3.4: Добавление ссылки в несуществующий чат")
    void test3_4_addToMissingChat() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            restClient().method(HttpMethod.POST).uri("/links")
                .header("Tg-Chat-Id", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddLinkRequest("https://github.com", null, null))
                .retrieve().toBodilessEntity();
        });
        assertTrue(ex.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Тест 3.5: Работа с удалённым чатом")
    void test3_5_workWithDeletedChat() {
        long chatId = 5L;

        restClient().method(HttpMethod.POST).uri("/tg-chat/{id}", chatId).retrieve().toBodilessEntity();
        restClient().method(HttpMethod.DELETE).uri("/tg-chat/{id}", chatId).retrieve().toBodilessEntity();

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            restClient().method(HttpMethod.POST).uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddLinkRequest("https://github.com", null, null))
                .retrieve().toBodilessEntity();
        });
        assertTrue(ex.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Тест 3.6: Удаление несуществующего чата")
    void test3_6_deleteMissingChat() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> {
            restClient().method(HttpMethod.DELETE).uri("/tg-chat/100500")
                .retrieve().toBodilessEntity();
        });
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
