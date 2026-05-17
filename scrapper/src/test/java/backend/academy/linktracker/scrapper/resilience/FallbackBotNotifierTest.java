package backend.academy.linktracker.scrapper.resilience;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.service.notifier.impl.FallbackBotNotifier;
import backend.academy.linktracker.scrapper.service.notifier.impl.SyncBotNotifier;
import backend.academy.linktracker.scrapper.service.notifier.impl.kafka.KafkaBotNotifier;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FallbackBotNotifierTest {

    @Mock private SyncBotNotifier httpNotifier;
    @Mock private KafkaBotNotifier kafkaNotifier;

    private CircuitBreaker circuitBreaker;
    private Retry retry;
    private FallbackBotNotifier notifier;
    private LinkUpdate linkUpdate;

    @BeforeEach
    void setUp() {
        circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
            .slidingWindowSize(3)
            .minimumNumberOfCalls(3)
            .failureRateThreshold(100)
            .permittedNumberOfCallsInHalfOpenState(2)
            .waitDurationInOpenState(Duration.ofMillis(200))
            .build());

        retry = Retry.of("test", RetryConfig.custom()
            .maxAttempts(2)
            .waitDuration(Duration.ZERO)
            .retryOnException(e -> e instanceof RuntimeException)
            .build());

        notifier = new FallbackBotNotifier(httpNotifier, kafkaNotifier, circuitBreaker, retry);

        linkUpdate = new LinkUpdate();
        linkUpdate.setId(1L);
        linkUpdate.setUrl(URI.create("https://github.com/test/repo"));
        linkUpdate.setDescription("test");
        linkUpdate.setTgChatIds(List.of(100L));
    }

    @Test
    @DisplayName("HTTP работает → Kafka НЕ вызывается")
    void whenHttpSucceeds_kafkaNotUsed() {
        doNothing().when(httpNotifier).notify(any());

        notifier.notify(linkUpdate);

        verify(httpNotifier, times(1)).notify(linkUpdate);
        verify(kafkaNotifier, never()).notify(any());
    }

    @Test
    @DisplayName("HTTP недоступен → используется Kafka как резервный транспорт")
    void whenHttpFails_kafkaUsedAsFallback() {
        doThrow(new RuntimeException("bot unavailable")).when(httpNotifier).notify(any());

        notifier.notify(linkUpdate);

        verify(httpNotifier, times(2)).notify(linkUpdate);
        verify(kafkaNotifier, times(1)).notify(linkUpdate);
    }

    @Test
    @DisplayName("Предохранитель переходит в состояние OPEN")
    void whenFailureRateExceeded_circuitBreakerOpens() {
        Retry singleAttempt = Retry.of("single", RetryConfig.custom()
            .maxAttempts(1).waitDuration(Duration.ZERO).build());
        FallbackBotNotifier sut = new FallbackBotNotifier(
            httpNotifier, kafkaNotifier, circuitBreaker, singleAttempt);

        doThrow(new RuntimeException("fail")).when(httpNotifier).notify(any());

        sut.notify(linkUpdate);
        sut.notify(linkUpdate);
        sut.notify(linkUpdate);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        reset(httpNotifier, kafkaNotifier);
        sut.notify(linkUpdate);

        verify(httpNotifier, never()).notify(any());
        verify(kafkaNotifier, times(1)).notify(linkUpdate);
    }

    @Test
    @DisplayName("Состояние HALF-OPEN → CLOSED при успешных пробных вызовах")
    void whenHalfOpenSuccessful_circuitBreakerCloses() throws InterruptedException {
        forceCircuitBreakerOpen();

        Thread.sleep(250);

        doNothing().when(httpNotifier).notify(any());

        notifier.notify(linkUpdate);
        notifier.notify(linkUpdate);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Состояние HALF-OPEN → OPEN при неуспешных пробных вызовах")
    void whenHalfOpenFails_circuitBreakerReopens() throws InterruptedException {
        forceCircuitBreakerOpen();

        Thread.sleep(250);

        doThrow(new RuntimeException("still down")).when(httpNotifier).notify(any());

        notifier.notify(linkUpdate);
        notifier.notify(linkUpdate);

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }


    private void forceCircuitBreakerOpen() {
        doThrow(new RuntimeException("fail")).when(httpNotifier).notify(any());
        Retry singleAttempt = Retry.of("force", RetryConfig.custom()
            .maxAttempts(1).waitDuration(Duration.ZERO).build());
        FallbackBotNotifier helper = new FallbackBotNotifier(
            httpNotifier, kafkaNotifier, circuitBreaker, singleAttempt);
        for (int i = 0; i < 3; i++) {
            helper.notify(linkUpdate);
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        reset(httpNotifier, kafkaNotifier);
    }
}
