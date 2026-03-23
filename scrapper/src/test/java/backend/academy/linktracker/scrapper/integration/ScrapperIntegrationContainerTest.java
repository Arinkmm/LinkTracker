package backend.academy.linktracker.scrapper.integration;

import static org.junit.jupiter.api.Assertions.*;

import backend.academy.linktracker.scrapper.configuration.E2ETestcontainersConfiguration;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = E2ETestcontainersConfiguration.class)
class ScrapperIntegrationContainerTest {

    @Autowired
    private GenericContainer<?> scrapperContainer;

    private RestClient restClient;

    private static final AtomicLong chatIdCounter = new AtomicLong(100L);

    @BeforeEach
    void setUp() {
        Integer port = scrapperContainer.getMappedPort(8081);
        this.restClient = RestClient.builder()
                .baseUrl("http://" + scrapperContainer.getHost() + ":" + port)
                .build();
    }

    private AddLinkRequest createAddLinkBody(String link) {
        AddLinkRequest request = new AddLinkRequest();
        request.setLink(URI.create(link));
        request.setTags(List.of());
        request.setFilters(List.of());
        return request;
    }

    @Test
    @DisplayName("Тест 3.1: Добавление и получение ссылки")
    void test3_1_addAndGetLink() {
        long chatId = chatIdCounter.getAndIncrement();
        String link = "https://github.com/user/repo";

        restClient
                .method(HttpMethod.POST)
                .uri("/tg-chat/{id}", chatId)
                .retrieve()
                .toBodilessEntity();

        restClient
                .method(HttpMethod.POST)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(createAddLinkBody(link))
                .retrieve()
                .toBodilessEntity();

        String response = restClient
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

        restClient
                .method(HttpMethod.POST)
                .uri("/tg-chat/{id}", chatId)
                .retrieve()
                .toBodilessEntity();

        restClient
                .method(HttpMethod.POST)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(createAddLinkBody(link))
                .retrieve()
                .toBodilessEntity();

        restClient
                .method(HttpMethod.DELETE)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(createAddLinkBody(link))
                .retrieve()
                .toBodilessEntity();

        String response = restClient
                .method(HttpMethod.GET)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .retrieve()
                .body(String.class);

        assertTrue(response != null && !response.contains(link));
    }

    @Test
    @DisplayName("Тест 3.3: Удаление ссылки из несуществующего чата")
    void test3_3_deleteFromMissingChat() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient
                .method(HttpMethod.DELETE)
                .uri("/links")
                .header("Tg-Chat-Id", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createAddLinkBody("https://yandex.ru"))
                .retrieve()
                .toBodilessEntity());
        assertTrue(ex.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Тест 3.4: Добавление ссылки в несуществующий чат")
    void test3_4_addToMissingChat() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient
                .method(HttpMethod.POST)
                .uri("/links")
                .header("Tg-Chat-Id", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createAddLinkBody("https://github.com"))
                .retrieve()
                .toBodilessEntity());
        assertTrue(ex.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Тест 3.5: Работа с удалённым чатом")
    void test3_5_workWithDeletedChat() {
        long chatId = chatIdCounter.getAndIncrement();

        restClient
                .method(HttpMethod.POST)
                .uri("/tg-chat/{id}", chatId)
                .retrieve()
                .toBodilessEntity();

        restClient
                .method(HttpMethod.DELETE)
                .uri("/tg-chat/{id}", chatId)
                .retrieve()
                .toBodilessEntity();

        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient
                .method(HttpMethod.POST)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(createAddLinkBody("https://github.com"))
                .retrieve()
                .toBodilessEntity());
        assertTrue(ex.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Тест 3.6: Удаление несуществующего чата")
    void test3_6_deleteMissingChat() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class, () -> restClient
                .method(HttpMethod.DELETE)
                .uri("/tg-chat/100500")
                .retrieve()
                .toBodilessEntity());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
