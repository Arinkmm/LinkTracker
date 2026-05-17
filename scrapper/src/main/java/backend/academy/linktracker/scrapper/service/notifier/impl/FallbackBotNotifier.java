package backend.academy.linktracker.scrapper.service.notifier.impl;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.exception.ApiException;
import backend.academy.linktracker.scrapper.service.notifier.BotNotifier;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class FallbackBotNotifier implements BotNotifier {
    private final BotNotifier primaryNotifier;
    private final BotNotifier kafkaNotifier;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    @Override
    public void notify(LinkUpdate linkUpdate) {
        Runnable retriedCall = Retry.decorateRunnable(retry, () -> primaryNotifier.notify(linkUpdate));
        Runnable decorated = CircuitBreaker.decorateRunnable(circuitBreaker, retriedCall);

        try {
            decorated.run();
            log.atDebug()
                    .addKeyValue("id", linkUpdate.getId())
                    .log("Notification sent via primary transport");
        } catch (CallNotPermittedException ex) {
            log.atWarn()
                    .addKeyValue("id", linkUpdate.getId())
                    .addKeyValue("state", circuitBreaker.getState())
                    .log("Primary bot notifier circuit breaker is open, falling back to Kafka");
            kafkaNotifier.notify(linkUpdate);
        } catch (ApiException ex) {
            log.atWarn()
                    .addKeyValue("id", linkUpdate.getId())
                    .addKeyValue("code", ex.getApiError().getCode())
                    .addKeyValue("description", ex.getApiError().getDescription())
                    .log("Primary bot notifier returned API error, falling back to Kafka");
            kafkaNotifier.notify(linkUpdate);
        } catch (RuntimeException ex) {
            log.atWarn()
                    .addKeyValue("id", linkUpdate.getId())
                    .addKeyValue("reason", ex.getClass().getSimpleName())
                    .addKeyValue("error", ex.getMessage())
                    .log("Primary bot notifier failed unexpectedly, falling back to Kafka");
            kafkaNotifier.notify(linkUpdate);
        }
    }
}
