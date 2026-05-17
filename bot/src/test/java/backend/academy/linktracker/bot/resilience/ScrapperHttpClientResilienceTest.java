package backend.academy.linktracker.bot.resilience;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.client.impl.ResilientScrapperClient;
import backend.academy.linktracker.bot.client.impl.ScrapperHttpClient;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.*;

class ScrapperHttpClientResilienceTest {
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private ScrapperClient client;

    private static final int TIMEOUT_MS = 1000;
    private static final Duration BACKOFF = Duration.ofMillis(150);
    private static final int MAX_ATTEMPTS = 3;

    @BeforeEach
    void setUp() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(TIMEOUT_MS))
                .setResponseTimeout(Timeout.ofMilliseconds(TIMEOUT_MS))
                .build();

        RestClient restClient = RestClient.builder()
                .baseUrl(wireMock.baseUrl())
                .requestFactory(new HttpComponentsClientHttpRequestFactory(
                        HttpClients.custom().setDefaultRequestConfig(requestConfig).build()))
                .build();

        CircuitBreaker cb = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
                .slidingWindowSize(100)
                .minimumNumberOfCalls(100)
                .failureRateThreshold(100)
                .build());

        Retry retry = Retry.of("test", RetryConfig.custom()
                .maxAttempts(MAX_ATTEMPTS)
                .waitDuration(BACKOFF)
                .retryOnException(ex -> ex instanceof HttpServerErrorException httpEx
                        && List.of(500, 502, 503, 504).contains(httpEx.getStatusCode().value()))
                .build());

        client = new ResilientScrapperClient(new ScrapperHttpClient(restClient), cb, retry);
    }

    @Test
    @DisplayName("Таймаут — сервис отвечает дольше настроенного времени ожидания")
    void whenServerTooSlow_requestTimesOut() {
        wireMock.stubFor(post(urlEqualTo("/tg-chat/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(TIMEOUT_MS * 3)));

        Instant start = Instant.now();

        assertThatThrownBy(() -> client.registerChat(1L))
                .isInstanceOf(Exception.class);

        long elapsed = Duration.between(start, Instant.now()).toMillis();
        assertThat(elapsed).isLessThan((long) TIMEOUT_MS * 2);
    }

    @Test
    @DisplayName("Повторный запрос на 5xx — итоговый ответ успешный")
    void whenServerReturns500ThenSuccess_retriesAndSucceeds() {
        wireMock.stubFor(post(urlEqualTo("/tg-chat/42"))
                .inScenario("retry")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("attempt-2"));

        wireMock.stubFor(post(urlEqualTo("/tg-chat/42"))
                .inScenario("retry")
                .whenScenarioStateIs("attempt-2")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("attempt-3"));

        wireMock.stubFor(post(urlEqualTo("/tg-chat/42"))
                .inScenario("retry")
                .whenScenarioStateIs("attempt-3")
                .willReturn(aResponse().withStatus(200)));

        assertThatCode(() -> client.registerChat(42L)).doesNotThrowAnyException();

        wireMock.verify(MAX_ATTEMPTS, postRequestedFor(urlEqualTo("/tg-chat/42")));
    }

    @Test
    @DisplayName("Повторный запрос не выполняется на 4xx")
    void whenServerReturns400_noRetry() {
        wireMock.stubFor(post(urlEqualTo("/tg-chat/99"))
                .willReturn(aResponse().withStatus(400)));

        assertThatThrownBy(() -> client.registerChat(99L))
                .isInstanceOf(HttpClientErrorException.class);

        wireMock.verify(1, postRequestedFor(urlEqualTo("/tg-chat/99")));
    }

    @Test
    @DisplayName("Постоянная задержка — интервал между попытками одинаковый")
    void whenRetryingWithConstantBackoff_intervalsAreEqual() {
        wireMock.stubFor(post(urlEqualTo("/tg-chat/77"))
                .willReturn(aResponse().withStatus(500)));

        long expectedMinMs = BACKOFF.toMillis() * (MAX_ATTEMPTS - 1);

        Instant start = Instant.now();
        assertThatThrownBy(() -> client.registerChat(77L))
                .isInstanceOf(Exception.class);
        long elapsed = Duration.between(start, Instant.now()).toMillis();

        assertThat(elapsed).isGreaterThanOrEqualTo(expectedMinMs);
        assertThat(elapsed).isLessThan(expectedMinMs + BACKOFF.toMillis() + 500);

        wireMock.verify(MAX_ATTEMPTS, postRequestedFor(urlEqualTo("/tg-chat/77")));
    }
}
