package backend.academy.linktracker.bot.client.impl;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.client.ScrapperTransportClient;
import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import backend.academy.linktracker.bot.exception.ApiException;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Primary
@Slf4j
@RequiredArgsConstructor
public class ResilientScrapperClient implements ScrapperClient {
    private final ScrapperTransportClient delegate;

    @Override
    @Retry(name = "scrapper-client")
    @CircuitBreaker(name = "scrapper-client", fallbackMethod = "registerChatFallback")
    public void registerChat(Long id) {
        delegate.registerChat(id);
    }

    @Override
    @Retry(name = "scrapper-client")
    @CircuitBreaker(name = "scrapper-client", fallbackMethod = "deleteChatFallback")
    public void deleteChat(Long id) {
        delegate.deleteChat(id);
    }

    @Override
    @Retry(name = "scrapper-client")
    @CircuitBreaker(name = "scrapper-client", fallbackMethod = "addLinkFallback")
    public LinkResponse addLink(Long id, URI url, List<String> tags, List<String> filters) {
        return delegate.addLink(id, url, tags, filters);
    }

    @Override
    @Retry(name = "scrapper-client")
    @CircuitBreaker(name = "scrapper-client", fallbackMethod = "removeLinkFallback")
    public LinkResponse removeLink(Long id, URI url) {
        return delegate.removeLink(id, url);
    }

    @Override
    @Retry(name = "scrapper-client")
    @CircuitBreaker(name = "scrapper-client", fallbackMethod = "getLinksFallback")
    public ListLinksResponse getLinks(Long id) {
        return delegate.getLinks(id);
    }

    private void registerChatFallback(Long id, CallNotPermittedException exception) {
        throw circuitBreakerOpen("registerChat", exception);
    }

    private void deleteChatFallback(Long id, CallNotPermittedException exception) {
        throw circuitBreakerOpen("deleteChat", exception);
    }

    private LinkResponse addLinkFallback(
            Long id, URI url, List<String> tags, List<String> filters, CallNotPermittedException exception) {
        throw circuitBreakerOpen("addLink", exception);
    }

    private LinkResponse removeLinkFallback(Long id, URI url, CallNotPermittedException exception) {
        throw circuitBreakerOpen("removeLink", exception);
    }

    private ListLinksResponse getLinksFallback(Long id, CallNotPermittedException exception) {
        throw circuitBreakerOpen("getLinks", exception);
    }

    private ApiException circuitBreakerOpen(String operation, CallNotPermittedException exception) {
        log.atWarn().setCause(exception).addKeyValue("operation", operation).log("Scrapper circuit breaker is open");

        ApiErrorResponse error = new ApiErrorResponse();
        error.setCode(String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value()));
        error.setDescription("Scrapper is temporarily unavailable");
        error.setExceptionName(exception.getClass().getSimpleName());
        error.setExceptionMessage(exception.getMessage());
        error.setStacktrace(List.of());
        return new ApiException(error);
    }
}
