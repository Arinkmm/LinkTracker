package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.client.api.github.GitHubClient;
import backend.academy.linktracker.scrapper.client.api.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.ResilienceProperties;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@RequiredArgsConstructor
public class ApiConfiguration {

    private final GithubProperties githubProperties;
    private final StackoverflowProperties stackoverflowProperties;
    private final ResilienceProperties resilienceProperties;

    @Bean
    public GitHubClient gitHubClient() {
        RestClient restClient = baseRestClientBuilder("github-api")
                .baseUrl(githubProperties.getUrl())
                .defaultHeader("Authorization", "Bearer " + githubProperties.getToken())
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();

        return createHttpProxy(restClient, GitHubClient.class);
    }

    @Bean
    public StackOverflowClient stackOverflowClient() {
        RestClient restClient = baseRestClientBuilder("stackoverflow-api")
                .baseUrl(stackoverflowProperties.getUrl())
                .build();

        return createHttpProxy(restClient, StackOverflowClient.class);
    }

    private RestClient.Builder baseRestClientBuilder(String name) {
        ResilienceProperties.Timeout timeout = resilienceProperties.getTimeout();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(timeout.getConnectTimeout()))
                .setConnectionRequestTimeout(Timeout.of(timeout.getConnectTimeout()))
                .setResponseTimeout(Timeout.of(timeout.getReadTimeout()))
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                HttpClients.custom().setDefaultRequestConfig(requestConfig).build());
        requestFactory.setReadTimeout(timeout.getReadTimeout());

        Retry retry = Retry.of(name, createRetryConfig());
        CircuitBreaker circuitBreaker = CircuitBreaker.of(name, createCircuitBreakerConfig());

        return RestClient.builder().requestFactory(requestFactory).requestInterceptor((request, body, execution) -> {
            try {
                return circuitBreaker.executeSupplier(
                        () -> retry.executeSupplier(() -> executeWithRetryableStatusCheck(request, body, execution)));
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        });
    }

    private <T> T createHttpProxy(RestClient restClient, Class<T> clientClass) {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(clientClass);
    }

    private ClientHttpResponse executeWithRetryableStatusCheck(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
        try {
            ClientHttpResponse response = execution.execute(request, body);
            if (!isRetryable(response)) {
                return response;
            }

            try {
                throw new RestClientResponseException(
                        "Retryable status code hit: " + response.getStatusCode(),
                        response.getStatusCode(),
                        response.getStatusText(),
                        response.getHeaders(),
                        response.getBody().readAllBytes(),
                        StandardCharsets.UTF_8);
            } finally {
                response.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private RetryConfig createRetryConfig() {
        ResilienceProperties.Retry retry = resilienceProperties.getRetry();
        return RetryConfig.custom()
                .maxAttempts(retry.getMaxAttempts())
                .intervalFunction(createIntervalFunction(retry))
                .retryOnException(throwable -> throwable instanceof IOException
                        || throwable instanceof UncheckedIOException
                        || throwable instanceof RestClientResponseException httpException
                                && retry.getRetryableStatusCodes()
                                        .contains(httpException.getStatusCode().value()))
                .build();
    }

    private CircuitBreakerConfig createCircuitBreakerConfig() {
        ResilienceProperties.CircuitBreaker cb = resilienceProperties.getCircuitBreaker();
        return CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(cb.getSlidingWindowSize())
                .minimumNumberOfCalls(cb.getMinimumNumberOfCalls())
                .failureRateThreshold(cb.getFailureRateThreshold())
                .permittedNumberOfCallsInHalfOpenState(cb.getPermittedCallsInHalfOpenState())
                .waitDurationInOpenState(cb.getWaitDurationInOpenState())
                .build();
    }

    private boolean isRetryable(ClientHttpResponse response) throws IOException {
        return resilienceProperties
                .getRetry()
                .getRetryableStatusCodes()
                .contains(response.getStatusCode().value());
    }

    private IntervalFunction createIntervalFunction(ResilienceProperties.Retry retry) {
        if ("exponential".equalsIgnoreCase(retry.getBackoffStrategy())) {
            return IntervalFunction.ofExponentialBackoff(retry.getWaitDuration(), retry.getExponentialMultiplier());
        }
        return IntervalFunction.of(retry.getWaitDuration());
    }
}
