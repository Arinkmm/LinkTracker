package backend.academy.linktracker.scrapper.service.notifier.impl;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.exception.ApiException;
import backend.academy.linktracker.scrapper.service.notifier.BotNotifier;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class FallbackBotNotifier implements BotNotifier {
    private final BotNotifier primaryNotifier;
    private final BotNotifier kafkaNotifier;

    @Override
    @Retry(name = "bot-notifier", fallbackMethod = "notifyViaKafka")
    @CircuitBreaker(name = "bot-notifier", fallbackMethod = "notifyWhenCircuitOpen")
    public void notify(LinkUpdate linkUpdate) {
        primaryNotifier.notify(linkUpdate);
        log.atDebug().addKeyValue("id", linkUpdate.getId()).log("Notification sent via primary transport");
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void notifyWhenCircuitOpen(LinkUpdate linkUpdate, CallNotPermittedException ex) {
        log.atWarn()
                .setCause(ex)
                .addKeyValue("id", linkUpdate.getId())
                .log("Primary bot notifier circuit breaker is open, falling back to Kafka");
        kafkaNotifier.notify(linkUpdate);
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void notifyViaKafka(LinkUpdate linkUpdate, RuntimeException ex) {
        if (ex instanceof ApiException apiException) {
            log.atWarn()
                    .setCause(apiException)
                    .addKeyValue("id", linkUpdate.getId())
                    .addKeyValue("code", apiException.getApiError().getCode())
                    .addKeyValue("description", apiException.getApiError().getDescription())
                    .log("Primary bot notifier returned API error, falling back to Kafka");
        } else {
            log.atWarn()
                    .setCause(ex)
                    .addKeyValue("id", linkUpdate.getId())
                    .addKeyValue("reason", ex.getClass().getSimpleName())
                    .addKeyValue("error", ex.getMessage())
                    .log("Primary bot notifier failed unexpectedly, falling back to Kafka");
        }
        kafkaNotifier.notify(linkUpdate);
    }
}
