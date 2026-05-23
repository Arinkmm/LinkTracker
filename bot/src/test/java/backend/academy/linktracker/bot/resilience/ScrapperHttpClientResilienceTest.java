package backend.academy.linktracker.bot.resilience;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.client.ScrapperTransportClient;
import backend.academy.linktracker.bot.client.impl.ResilientScrapperClient;
import backend.academy.linktracker.bot.client.impl.ScrapperHttpClient;
import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import backend.academy.linktracker.bot.exception.ApiException;
import backend.academy.linktracker.bot.exception.RetryableApiException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@SpringBootTest(
        classes = ScrapperHttpClientResilienceTest.TestConfig.class,
        properties = {
            "spring.aop.proxy-target-class=true",
            "resilience4j.retry.retry-aspect-order=1",
            "resilience4j.circuitbreaker.circuit-breaker-aspect-order=2",
            "resilience4j.retry.instances.scrapper-client.max-attempts=3",
            "resilience4j.retry.instances.scrapper-client.wait-duration=150ms",
            "resilience4j.retry.instances.scrapper-client.retry-exceptions="
                    + "backend.academy.linktracker.bot.exception.RetryableApiException",
            "resilience4j.circuitbreaker.instances.scrapper-client.sliding-window-type=COUNT_BASED",
            "resilience4j.circuitbreaker.instances.scrapper-client.sliding-window-size=10",
            "resilience4j.circuitbreaker.instances.scrapper-client.minimum-number-of-calls=5",
            "resilience4j.circuitbreaker.instances.scrapper-client.failure-rate-threshold=50",
            "resilience4j.circuitbreaker.instances.scrapper-client.permitted-number-of-calls-in-half-open-state=5",
            "resilience4j.circuitbreaker.instances.scrapper-client.wait-duration-in-open-state=1s"
        })
@ImportAutoConfiguration({
    AopAutoConfiguration.class,
    RetryAutoConfiguration.class,
    CircuitBreakerAutoConfiguration.class
})
class ScrapperHttpClientResilienceTest {
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final int TIMEOUT_MS = 1000;
    private static final Duration BACKOFF = Duration.ofMillis(150);
    private static final int MAX_ATTEMPTS = 3;

    @Autowired
    private ScrapperClient client;

    @Autowired
    private ScrapperTransportClient delegate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        reset(delegate);
        circuitBreakerRegistry.circuitBreaker("scrapper-client").reset();
    }

    @Test
    @DisplayName("Timeout: сервис отвечает дольше настроенного времени ожидания")
    void whenServerTooSlow_requestTimesOut() {
        wireMock.stubFor(post(urlEqualTo("/tg-chat/1"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(TIMEOUT_MS * 3)));

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(TIMEOUT_MS))
                .setResponseTimeout(Timeout.ofMilliseconds(TIMEOUT_MS))
                .build();
        RestClient restClient = RestClient.builder()
                .baseUrl(wireMock.baseUrl())
                .requestFactory(new HttpComponentsClientHttpRequestFactory(HttpClients.custom()
                        .setDefaultRequestConfig(requestConfig)
                        .build()))
                .build();
        ScrapperHttpClient httpClient = new ScrapperHttpClient(restClient);

        Instant start = Instant.now();

        assertThatThrownBy(() -> httpClient.registerChat(1L)).isInstanceOf(Exception.class);

        long elapsed = Duration.between(start, Instant.now()).toMillis();
        assertThat(elapsed).isLessThan((long) TIMEOUT_MS * 2);
    }

    @Test
    @DisplayName("Retry: retryable-ошибка повторяется и итоговый вызов успешен")
    void whenRetryableErrorThenSuccess_retriesAndSucceeds() {
        doThrow(apiException(HttpStatus.INTERNAL_SERVER_ERROR))
                .doThrow(apiException(HttpStatus.INTERNAL_SERVER_ERROR))
                .doNothing()
                .when(delegate)
                .registerChat(42L);

        assertThatCode(() -> client.registerChat(42L)).doesNotThrowAnyException();

        verify(delegate, times(MAX_ATTEMPTS)).registerChat(42L);
    }

    @Test
    @DisplayName("Retry: non-retryable ошибка не повторяется")
    void whenNonRetryableError_noRetry() {
        doThrow(apiException(HttpStatus.BAD_REQUEST)).when(delegate).registerChat(99L);

        assertThatThrownBy(() -> client.registerChat(99L)).isInstanceOf(ApiException.class);

        verify(delegate, times(1)).registerChat(99L);
    }

    @Test
    @DisplayName("Retry: constant backoff выдерживает одинаковую задержку между попытками")
    void whenRetryingWithConstantBackoff_intervalsAreEqual() {
        doThrow(apiException(HttpStatus.INTERNAL_SERVER_ERROR)).when(delegate).registerChat(77L);

        long expectedMinMs = BACKOFF.toMillis() * (MAX_ATTEMPTS - 1);

        Instant start = Instant.now();
        assertThatThrownBy(() -> client.registerChat(77L)).isInstanceOf(RetryableApiException.class);
        long elapsed = Duration.between(start, Instant.now()).toMillis();

        assertThat(elapsed).isGreaterThanOrEqualTo(expectedMinMs);
        assertThat(elapsed).isLessThan(expectedMinMs + BACKOFF.toMillis() + 500);

        verify(delegate, times(MAX_ATTEMPTS)).registerChat(77L);
    }

    private static ApiException apiException(HttpStatus status) {
        ApiErrorResponse error = new ApiErrorResponse();
        error.setCode(String.valueOf(status.value()));
        error.setDescription(status.getReasonPhrase());
        error.setExceptionName(
                status.is5xxServerError()
                        ? RetryableApiException.class.getSimpleName()
                        : ApiException.class.getSimpleName());
        error.setExceptionMessage(status.getReasonPhrase());
        error.setStacktrace(List.of());
        if (status.is5xxServerError()) {
            return new RetryableApiException(error);
        }
        return new ApiException(error);
    }

    @TestConfiguration
    @Import(ResilientScrapperClient.class)
    static class TestConfig {
        @Bean
        ScrapperTransportClient scrapperTransportClient() {
            return mock(ScrapperTransportClient.class);
        }
    }
}
