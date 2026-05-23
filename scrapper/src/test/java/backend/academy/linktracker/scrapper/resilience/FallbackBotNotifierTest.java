package backend.academy.linktracker.scrapper.resilience;

import static backend.academy.linktracker.scrapper.resilience.FallbackBotNotifierTestConfiguration.KAFKA_NOTIFIER;
import static backend.academy.linktracker.scrapper.resilience.FallbackBotNotifierTestConfiguration.PRIMARY_NOTIFIER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import backend.academy.linktracker.scrapper.exception.RetryableApiException;
import backend.academy.linktracker.scrapper.service.notifier.BotNotifier;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

@SpringBootTest(classes = FallbackBotNotifierTestConfiguration.class)
@ImportAutoConfiguration({
    AopAutoConfiguration.class,
    RetryAutoConfiguration.class,
    CircuitBreakerAutoConfiguration.class
})
class FallbackBotNotifierTest {
    @Autowired
    private BotNotifier notifier;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    private CircuitBreaker circuitBreaker;
    private Retry retry;
    private LinkUpdate linkUpdate;

    @BeforeEach
    void setUp() {
        reset(PRIMARY_NOTIFIER, KAFKA_NOTIFIER);
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("bot-notifier");
        retry = retryRegistry.retry("bot-notifier");
        circuitBreaker.reset();

        linkUpdate = new LinkUpdate();
        linkUpdate.setId(1L);
        linkUpdate.setUrl(URI.create("https://github.com/test/repo"));
        linkUpdate.setDescription("test");
        linkUpdate.setTgChatIds(List.of(100L));
    }

    @Test
    @DisplayName("Fallback: HTTP работает, Kafka не вызывается")
    void whenHttpSucceeds_kafkaNotUsed() {
        doNothing().when(PRIMARY_NOTIFIER).notify(linkUpdate);

        notifier.notify(linkUpdate);

        verify(PRIMARY_NOTIFIER, times(1)).notify(linkUpdate);
        verify(KAFKA_NOTIFIER, never()).notify(linkUpdate);
    }

    @Test
    @DisplayName("Fallback: HTTP недоступен, используется Kafka")
    void whenHttpFails_kafkaUsedAsFallback() {
        doThrow(retryableApiException()).when(PRIMARY_NOTIFIER).notify(linkUpdate);

        notifier.notify(linkUpdate);

        verify(PRIMARY_NOTIFIER, times(retry.getRetryConfig().getMaxAttempts())).notify(linkUpdate);
        verify(KAFKA_NOTIFIER, times(1)).notify(linkUpdate);
    }

    @Test
    @DisplayName("Circuit Breaker: при превышении процента ошибок переходит в OPEN")
    void whenFailureRateExceeded_circuitBreakerOpens() {
        forceCircuitBreakerOpen();

        reset(PRIMARY_NOTIFIER, KAFKA_NOTIFIER);
        notifier.notify(linkUpdate);

        verify(PRIMARY_NOTIFIER, never()).notify(linkUpdate);
        verify(KAFKA_NOTIFIER, times(1)).notify(linkUpdate);
    }

    @Test
    @DisplayName("Circuit Breaker: HALF-OPEN переходит в CLOSED после успешных пробных вызовов")
    void whenHalfOpenSuccessful_circuitBreakerCloses() throws InterruptedException {
        forceCircuitBreakerOpen();
        waitUntilHalfOpenCallsAreAllowed();

        doNothing().when(PRIMARY_NOTIFIER).notify(linkUpdate);

        int permittedCalls = circuitBreaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState();
        for (int i = 0; i < permittedCalls; i++) {
            notifier.notify(linkUpdate);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Circuit Breaker: HALF-OPEN возвращается в OPEN после неуспешных пробных вызовов")
    void whenHalfOpenFails_circuitBreakerReopens() throws InterruptedException {
        forceCircuitBreakerOpen();
        waitUntilHalfOpenCallsAreAllowed();

        doThrow(retryableApiException()).when(PRIMARY_NOTIFIER).notify(linkUpdate);

        int maxCalls = circuitBreaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState() + 1;
        for (int i = 0; i < maxCalls && circuitBreaker.getState() != CircuitBreaker.State.OPEN; i++) {
            notifier.notify(linkUpdate);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    private void forceCircuitBreakerOpen() {
        doThrow(retryableApiException()).when(PRIMARY_NOTIFIER).notify(linkUpdate);

        int maxCalls = circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize() + 1;
        for (int i = 0; i < maxCalls && circuitBreaker.getState() != CircuitBreaker.State.OPEN; i++) {
            notifier.notify(linkUpdate);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        reset(PRIMARY_NOTIFIER, KAFKA_NOTIFIER);
    }

    private void waitUntilHalfOpenCallsAreAllowed() throws InterruptedException {
        long waitMillis = circuitBreaker
                .getCircuitBreakerConfig()
                .getWaitIntervalFunctionInOpenState()
                .apply(1);
        Thread.sleep(Duration.ofMillis(waitMillis).plusMillis(50).toMillis());
    }

    private static RetryableApiException retryableApiException() {
        ApiErrorResponse error = new ApiErrorResponse();
        error.setCode(String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value()));
        error.setDescription("Bot unavailable");
        error.setExceptionName(RetryableApiException.class.getSimpleName());
        error.setExceptionMessage("Bot unavailable");
        error.setStacktrace(List.of());
        return new RetryableApiException(error);
    }
}
