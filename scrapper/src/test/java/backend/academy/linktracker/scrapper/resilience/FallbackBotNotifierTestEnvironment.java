package backend.academy.linktracker.scrapper.resilience;

import static org.mockito.Mockito.mock;

import backend.academy.linktracker.scrapper.service.notifier.BotNotifier;
import backend.academy.linktracker.scrapper.service.notifier.impl.FallbackBotNotifier;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FallbackBotNotifierTestEnvironment.TestConfig.class)
@ImportAutoConfiguration({
    AopAutoConfiguration.class,
    RetryAutoConfiguration.class,
    CircuitBreakerAutoConfiguration.class
})
abstract class FallbackBotNotifierTestEnvironment {
    static final BotNotifier PRIMARY_NOTIFIER = mock(BotNotifier.class);
    static final BotNotifier KAFKA_NOTIFIER = mock(BotNotifier.class);

    @DynamicPropertySource
    static void botNotifierProperties(DynamicPropertyRegistry registry) {
        registry.add("resilience4j.retry.configs.default.retryable-status-codes[0]", () -> "500");
        registry.add("resilience4j.retry.configs.default.retryable-status-codes[1]", () -> "502");
        registry.add("resilience4j.retry.configs.default.retryable-status-codes[2]", () -> "503");
        registry.add("resilience4j.retry.configs.default.retryable-status-codes[3]", () -> "504");
        registry.add(
                "resilience4j.retry.configs.default.retry-exceptions[0]",
                () -> "backend.academy.linktracker.scrapper.exception.RetryableApiException");
        registry.add(
                "resilience4j.retry.configs.default.retry-exceptions[1]",
                () -> "org.springframework.web.client.ResourceAccessException");
        registry.add("resilience4j.retry.configs.default.retry-exceptions[2]", () -> "java.io.IOException");
        registry.add("resilience4j.retry.configs.default.retry-exceptions[3]", () -> "java.io.UncheckedIOException");
        registry.add("resilience4j.retry.instances.bot-notifier.base-config", () -> "default");
        registry.add("resilience4j.retry.instances.bot-notifier.max-attempts", () -> "2");
        registry.add("resilience4j.retry.instances.bot-notifier.wait-duration", () -> "0ms");
        registry.add("resilience4j.circuitbreaker.instances.bot-notifier.sliding-window-type", () -> "COUNT_BASED");
        registry.add("resilience4j.circuitbreaker.instances.bot-notifier.sliding-window-size", () -> "3");
        registry.add("resilience4j.circuitbreaker.instances.bot-notifier.minimum-number-of-calls", () -> "3");
        registry.add("resilience4j.circuitbreaker.instances.bot-notifier.failure-rate-threshold", () -> "100");
        registry.add(
                "resilience4j.circuitbreaker.instances.bot-notifier.permitted-number-of-calls-in-half-open-state",
                () -> "2");
        registry.add("resilience4j.circuitbreaker.instances.bot-notifier.wait-duration-in-open-state", () -> "200ms");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        BotNotifier testPrimaryNotifier() {
            return PRIMARY_NOTIFIER;
        }

        @Bean
        BotNotifier testKafkaNotifier() {
            return KAFKA_NOTIFIER;
        }

        @Bean
        FallbackBotNotifier testFallbackBotNotifier() {
            return new FallbackBotNotifier(testPrimaryNotifier(), testKafkaNotifier());
        }
    }
}
