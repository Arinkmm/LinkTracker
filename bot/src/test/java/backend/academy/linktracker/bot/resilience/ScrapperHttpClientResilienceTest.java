package backend.academy.linktracker.bot.resilience;

import static backend.academy.linktracker.bot.resilience.ScrapperHttpClientResilienceTestEnvironment.SCRAPPER_TRANSPORT_CLIENT;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.client.ScrapperTransportClient;
import backend.academy.linktracker.bot.client.impl.ScrapperHttpClient;
import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import backend.academy.linktracker.bot.exception.ApiException;
import backend.academy.linktracker.bot.exception.RetryableApiException;
import backend.academy.linktracker.bot.properties.ClientTimeoutProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.functions.Either;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;

class ScrapperHttpClientResilienceTest extends ScrapperHttpClientResilienceTestEnvironment {
    @Autowired
    @Qualifier("resilientScrapperClient")
    private ScrapperClient client;

    @Autowired
    @Qualifier("scrapperTransportClient")
    private ScrapperTransportClient delegate;

    @Autowired
    private ScrapperHttpClient httpClient;

    @Autowired
    private ClientTimeoutProperties timeoutProperties;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    private Retry retry;

    @BeforeEach
    void setUp() {
        reset(SCRAPPER_TRANSPORT_CLIENT);
        circuitBreakerRegistry.circuitBreaker("scrapper-client").reset();
        retry = retryRegistry.retry("scrapper-client");
    }

    @Test
    @DisplayName("Timeout: сервис отвечает дольше настроенного времени ожидания")
    void whenServerTooSlow_requestTimesOut() {
        long timeoutMs = timeoutProperties.getReadTimeout().toMillis();
        wireMock.stubFor(post(urlEqualTo("/tg-chat/1"))
                .willReturn(aResponse().withStatus(200).withFixedDelay((int) timeoutMs * 3)));

        Instant start = Instant.now();

        assertThatThrownBy(() -> httpClient.registerChat(1L)).isInstanceOf(Exception.class);

        long elapsed = Duration.between(start, Instant.now()).toMillis();
        assertThat(elapsed).isLessThan(timeoutMs * 2);
    }

    @Test
    @DisplayName("Retry: retryable-ошибка повторяется, итоговый вызов успешен")
    void whenRetryableErrorThenSuccess_retriesAndSucceeds() {
        doThrow(apiException(HttpStatus.INTERNAL_SERVER_ERROR))
                .doThrow(apiException(HttpStatus.INTERNAL_SERVER_ERROR))
                .doNothing()
                .when(delegate)
                .registerChat(42L);

        assertThatCode(() -> client.registerChat(42L)).doesNotThrowAnyException();

        verify(delegate, times(retry.getRetryConfig().getMaxAttempts())).registerChat(42L);
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

        long backoffMs = retryBackoffMs();
        long expectedMinMs = backoffMs * (retry.getRetryConfig().getMaxAttempts() - 1);

        Instant start = Instant.now();
        assertThatThrownBy(() -> client.registerChat(77L)).isInstanceOf(RetryableApiException.class);
        long elapsed = Duration.between(start, Instant.now()).toMillis();

        assertThat(elapsed).isGreaterThanOrEqualTo(expectedMinMs);
        assertThat(elapsed).isLessThan(expectedMinMs + backoffMs + 500);

        verify(delegate, times(retry.getRetryConfig().getMaxAttempts())).registerChat(77L);
    }

    private long retryBackoffMs() {
        Function<Integer, Long> intervalFunction = retry.getRetryConfig().getIntervalFunction();
        if (intervalFunction != null) {
            return intervalFunction.apply(1);
        }
        return retry.getRetryConfig().getIntervalBiFunction().apply(1, Either.right(null));
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
}
