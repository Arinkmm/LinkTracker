package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.exception.ApiException;
import backend.academy.linktracker.scrapper.properties.ResilienceProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

@Configuration
public class ResilienceConfiguration {
    @Bean
    public CircuitBreaker botNotifierCircuitBreaker(ResilienceProperties props) {
        ResilienceProperties.CircuitBreaker circuitBreaker = props.getCircuitBreaker();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(circuitBreaker.getSlidingWindowSize())
                .minimumNumberOfCalls(circuitBreaker.getMinimumNumberOfCalls())
                .failureRateThreshold(circuitBreaker.getFailureRateThreshold())
                .permittedNumberOfCallsInHalfOpenState(circuitBreaker.getPermittedCallsInHalfOpenState())
                .waitDurationInOpenState(circuitBreaker.getWaitDurationInOpenState())
                .build();

        return CircuitBreakerRegistry.of(config).circuitBreaker("bot-notifier");
    }

    @Bean
    public Retry botNotifierRetry(ResilienceProperties props) {
        ResilienceProperties.Retry retry = props.getRetry();

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(retry.getMaxAttempts())
                .intervalFunction(intervalFunction(retry))
                .retryOnException(ex -> isRetryable(ex, retry))
                .build();

        return RetryRegistry.of(config).retry("bot-notifier");
    }

    private boolean isRetryable(Throwable ex, ResilienceProperties.Retry retry) {
        if (ex instanceof HttpStatusCodeException httpEx) {
            return retry.getRetryableStatusCodes()
                    .contains(httpEx.getStatusCode().value());
        }
        if (ex instanceof ApiException apiEx) {
            return isRetryableApiError(apiEx, retry);
        }
        return ex instanceof IOException || ex instanceof ResourceAccessException;
    }

    private IntervalFunction intervalFunction(ResilienceProperties.Retry retry) {
        if ("exponential".equalsIgnoreCase(retry.getBackoffStrategy())) {
            return IntervalFunction.ofExponentialBackoff(retry.getWaitDuration(), retry.getExponentialMultiplier());
        }
        return IntervalFunction.of(retry.getWaitDuration());
    }

    private boolean isRetryableApiError(ApiException exception, ResilienceProperties.Retry retry) {
        try {
            return retry.getRetryableStatusCodes()
                    .contains(Integer.parseInt(exception.getApiError().getCode()));
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
