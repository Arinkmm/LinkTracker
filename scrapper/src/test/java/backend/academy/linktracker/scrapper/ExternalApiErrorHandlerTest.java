package backend.academy.linktracker.scrapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.github.GitHubClient;
import backend.academy.linktracker.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.service.provider.impl.GitHubTimeProvider;
import backend.academy.linktracker.scrapper.service.provider.impl.StackOverflowTimeProvider;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExternalApiErrorHandlerTest {

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private StackOverflowClient stackOverflowClient;

    @InjectMocks
    private GitHubTimeProvider gitHubTimeProvider;

    @InjectMocks
    private StackOverflowTimeProvider stackOverflowTimeProvider;

    @Test
    @DisplayName("GitHub: ошибка API (500/404) -> возвращает EPOCH, не падает")
    void gitHub_ApiError_ReturnsEpoch() {
        when(gitHubClient.getRepository(anyString(), anyString())).thenThrow(new RestClientException("GitHub is down"));

        assertDoesNotThrow(() -> {
            Instant result = gitHubTimeProvider.getCurrentTime(URI.create("https://github.com/user/repo"));

            assertEquals(Instant.EPOCH, result);
        });
    }

    @Test
    @DisplayName("GitHub: некорректный формат ответа (NullPointerException) -> возвращает EPOCH")
    void gitHub_MalformedResponse_ReturnsEpoch() {
        when(gitHubClient.getRepository(anyString(), anyString())).thenReturn(null);

        Instant result = gitHubTimeProvider.getCurrentTime(URI.create("https://github.com/user/repo"));

        assertEquals(Instant.EPOCH, result);
    }

    @Test
    @DisplayName("StackOverflow: ошибка API -> возвращает EPOCH, не падает")
    void stackOverflow_ApiError_ReturnsEpoch() {
        when(stackOverflowClient.getQuestion(anyString()))
                .thenThrow(new RuntimeException("StackOverflow rate limit exceeded"));

        Instant result =
                stackOverflowTimeProvider.getCurrentTime(URI.create("https://stackoverflow.com/questions/123/title"));

        assertEquals(Instant.EPOCH, result);
    }

    @Test
    @DisplayName("GitHub: некорректная ссылка (ошибка парсинга) -> возвращает EPOCH")
    void gitHub_InvalidUrlFormat_ReturnsEpoch() {
        Instant result = gitHubTimeProvider.getCurrentTime(URI.create("https://github.com/only_one_part"));

        assertEquals(Instant.EPOCH, result);
    }
}
