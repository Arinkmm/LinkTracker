package backend.academy.linktracker.bot.client.impl;

import static backend.academy.linktracker.bot.exception.ApiExceptionMapper.circuitBreakerOpen;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.client.ScrapperTransportClient;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.net.URI;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@Slf4j
public class ResilientScrapperClient implements ScrapperClient {
    private final ScrapperTransportClient delegate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public ResilientScrapperClient(
            ScrapperTransportClient delegate,
            CircuitBreaker circuitBreaker,
            Retry retry) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
    }

    @Override
    public void registerChat(Long id) {
        runWithResilience("registerChat", () -> delegate.registerChat(id));
    }

    @Override
    public void deleteChat(Long id) {
        runWithResilience("deleteChat", () -> delegate.deleteChat(id));
    }

    @Override
    public LinkResponse addLink(Long id, URI url, List<String> tags, List<String> filters) {
        return withResilience("addLink", () -> delegate.addLink(id, url, tags, filters));
    }

    @Override
    public LinkResponse removeLink(Long id, URI url) {
        return withResilience("removeLink", () -> delegate.removeLink(id, url));
    }

    @Override
    public ListLinksResponse getLinks(Long id) {
        return withResilience("getLinks", () -> delegate.getLinks(id));
    }

    private void runWithResilience(String operation, Runnable call) {
        withResilience(operation, () -> {
            call.run();
            return null;
        });
    }

    private <T> T withResilience(String operation, Supplier<T> call) {
        Supplier<T> retriedCall = Retry.decorateSupplier(retry, call);
        try {
            return CircuitBreaker.decorateSupplier(circuitBreaker, retriedCall).get();
        } catch (CallNotPermittedException e) {
            log.atWarn()
                    .addKeyValue("operation", operation)
                    .addKeyValue("state", circuitBreaker.getState())
                    .log("Scrapper circuit breaker is open");
            throw circuitBreakerOpen(e, "Scrapper");
        }
    }
}
