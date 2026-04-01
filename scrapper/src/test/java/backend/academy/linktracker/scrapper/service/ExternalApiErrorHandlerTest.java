package backend.academy.linktracker.scrapper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.github.GitHubClient;
import backend.academy.linktracker.scrapper.client.github.GitHubRepoResponse;
import backend.academy.linktracker.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.service.provider.impl.GitHubTimeProvider;
import backend.academy.linktracker.scrapper.service.provider.impl.StackOverflowTimeProvider;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private GitHubTimeProvider gitHubTimeProvider;
    private StackOverflowTimeProvider stackOverflowTimeProvider;

    @BeforeEach
    void setUp() {
        gitHubTimeProvider = new GitHubTimeProvider(gitHubClient);
        stackOverflowTimeProvider = new StackOverflowTimeProvider(stackOverflowClient);
    }

    @Test
    @DisplayName("GitHub: ошибка API (500/404) -> возвращает Optional.empty()")
    void gitHub_ApiError_ReturnsEmpty() {
        when(gitHubClient.getRepository(anyString(), anyString())).thenThrow(new RestClientException("GitHub is down"));

        Optional<Instant> result = gitHubTimeProvider.getCurrentTime(URI.create("https://github.com/user/repo"));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("GitHub: некорректный формат (null) -> возвращает Optional.empty()")
    void gitHub_MalformedResponse_ReturnsEmpty() {
        when(gitHubClient.getRepository(anyString(), anyString())).thenReturn(null);

        Optional<Instant> result = gitHubTimeProvider.getCurrentTime(URI.create("https://github.com/user/repo"));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("StackOverflow: ошибка API -> возвращает Optional.empty()")
    void stackOverflow_ApiError_ReturnsEmpty() {
        when(stackOverflowClient.getQuestion(anyString())).thenThrow(new RuntimeException("SO rate limit"));

        Optional<Instant> result =
                stackOverflowTimeProvider.getCurrentTime(URI.create("https://stackoverflow.com/questions/1/title"));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("GitHub: некорректная ссылка (ошибка парсинга) -> возвращает Optional.empty()")
    void gitHub_InvalidUrlFormat_ReturnsEmpty() {
        Optional<Instant> result = gitHubTimeProvider.getCurrentTime(URI.create("https://github.com/only_one_part"));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("GitHub: успех -> возвращает Optional с Instant")
    void gitHub_Success_ReturnsOptionalWithTime() {
        Instant now = Instant.now();
        when(gitHubClient.getRepository("user", "repo")).thenReturn(new GitHubRepoResponse(now));

        Optional<Instant> result = gitHubTimeProvider.getCurrentTime(URI.create("https://github.com/user/repo"));

        assertTrue(result.isPresent());
        assertEquals(now, result.get());
    }
}
