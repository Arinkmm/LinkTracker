package backend.academy.linktracker.bot.resilience;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.Mockito.mock;

import backend.academy.linktracker.bot.client.ScrapperTransportClient;
import backend.academy.linktracker.bot.client.impl.ResilientScrapperClient;
import backend.academy.linktracker.bot.client.impl.ScrapperHttpClient;
import backend.academy.linktracker.bot.configuration.ClientConfiguration;
import backend.academy.linktracker.bot.exception.ScrapperClientExceptionFactory;
import backend.academy.linktracker.bot.properties.ClientTimeoutProperties;
import backend.academy.linktracker.bot.properties.MessagesProperties;
import backend.academy.linktracker.bot.properties.RetryProperties;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ScrapperHttpClientResilienceTestEnvironment.TestConfig.class)
@ImportAutoConfiguration({
    AopAutoConfiguration.class,
    RetryAutoConfiguration.class,
    CircuitBreakerAutoConfiguration.class
})
abstract class ScrapperHttpClientResilienceTestEnvironment {
    static final ScrapperTransportClient SCRAPPER_TRANSPORT_CLIENT = mock(ScrapperTransportClient.class);

    @RegisterExtension
    protected static final WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void scrapperClientProperties(DynamicPropertyRegistry registry) {
        registry.add("app.scrapper.url", wireMock::baseUrl);
        registry.add("app.scrapper-client.type", () -> "http");
        registry.add("app.client.timeout.connect-timeout", () -> "1s");
        registry.add("app.client.timeout.read-timeout", () -> "1s");
        registry.add("resilience4j.retry.configs.default.retryable-status-codes[0]", () -> "500");
        registry.add("resilience4j.retry.configs.default.retryable-status-codes[1]", () -> "502");
        registry.add("resilience4j.retry.configs.default.retryable-status-codes[2]", () -> "503");
        registry.add("resilience4j.retry.configs.default.retryable-status-codes[3]", () -> "504");
        registry.add(
                "resilience4j.retry.configs.default.retry-exceptions[0]",
                () -> "backend.academy.linktracker.bot.exception.RetryableApiException");
        registry.add(
                "resilience4j.retry.configs.default.retry-exceptions[1]",
                () -> "org.springframework.web.client.ResourceAccessException");
        registry.add("resilience4j.retry.configs.default.retry-exceptions[2]", () -> "java.io.IOException");
        registry.add("resilience4j.retry.configs.default.retry-exceptions[3]", () -> "java.io.UncheckedIOException");
        registry.add("resilience4j.retry.instances.scrapper-client.base-config", () -> "default");
        registry.add("resilience4j.retry.instances.scrapper-client.max-attempts", () -> "3");
        registry.add("resilience4j.retry.instances.scrapper-client.wait-duration", () -> "150ms");
    }

    @TestConfiguration
    @EnableConfigurationProperties({ClientTimeoutProperties.class, MessagesProperties.class, RetryProperties.class})
    @Import({ClientConfiguration.class, ScrapperClientExceptionFactory.class})
    static class TestConfig {
        @Bean
        ScrapperTransportClient scrapperTransportClient() {
            return SCRAPPER_TRANSPORT_CLIENT;
        }

        @Bean
        ResilientScrapperClient resilientScrapperClient() {
            return new ResilientScrapperClient(SCRAPPER_TRANSPORT_CLIENT);
        }

        @Bean
        ScrapperHttpClient scrapperHttpClient(RestClient scrapperRestClient) {
            return new ScrapperHttpClient(scrapperRestClient);
        }
    }
}
