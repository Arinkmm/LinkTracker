package backend.academy.linktracker.bot.resilience;

import static org.mockito.Mockito.mock;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.client.ScrapperTransportClient;
import backend.academy.linktracker.bot.client.impl.ResilientScrapperClient;
import backend.academy.linktracker.bot.client.impl.ScrapperHttpClient;
import backend.academy.linktracker.bot.configuration.ClientConfiguration;
import backend.academy.linktracker.bot.exception.ScrapperClientExceptionFactory;
import backend.academy.linktracker.bot.properties.ClientTimeoutProperties;
import backend.academy.linktracker.bot.properties.MessagesProperties;
import backend.academy.linktracker.bot.properties.RetryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({ClientTimeoutProperties.class, MessagesProperties.class, RetryProperties.class})
@Import({ClientConfiguration.class, ScrapperClientExceptionFactory.class})
class ScrapperHttpClientResilienceTestConfiguration {
    static final ScrapperTransportClient SCRAPPER_TRANSPORT_CLIENT = mock(ScrapperTransportClient.class);

    @Bean
    ScrapperTransportClient scrapperTransportClient() {
        return SCRAPPER_TRANSPORT_CLIENT;
    }

    @Bean
    ScrapperClient resilientScrapperClient() {
        return new ResilientScrapperClient(SCRAPPER_TRANSPORT_CLIENT);
    }

    @Bean
    ScrapperHttpClient scrapperHttpClient(RestClient scrapperRestClient) {
        return new ScrapperHttpClient(scrapperRestClient);
    }
}
