package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.client.api.github.GitHubClient;
import backend.academy.linktracker.scrapper.client.api.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.exception.ExternalApiExceptionFactory;
import backend.academy.linktracker.scrapper.properties.ClientTimeoutProperties;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@RequiredArgsConstructor
public class ApiConfiguration {

    private final GithubProperties githubProperties;
    private final StackoverflowProperties stackoverflowProperties;
    private final ClientTimeoutProperties clientTimeoutProperties;
    private final ExternalApiExceptionFactory exceptionFactory;

    @Bean
    public GitHubClient gitHubClient() {
        RestClient restClient = baseRestClientBuilder()
                .baseUrl(githubProperties.getUrl())
                .defaultHeader("Authorization", "Bearer " + githubProperties.getToken())
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();

        return createHttpProxy(restClient, GitHubClient.class);
    }

    @Bean
    public StackOverflowClient stackOverflowClient() {
        RestClient restClient = baseRestClientBuilder()
                .baseUrl(stackoverflowProperties.getUrl())
                .build();

        return createHttpProxy(restClient, StackOverflowClient.class);
    }

    private RestClient.Builder baseRestClientBuilder() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(clientTimeoutProperties.getConnectTimeout()))
                .setConnectionRequestTimeout(Timeout.of(clientTimeoutProperties.getConnectTimeout()))
                .setResponseTimeout(Timeout.of(clientTimeoutProperties.getReadTimeout()))
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                HttpClients.custom().setDefaultRequestConfig(requestConfig).build());
        requestFactory.setReadTimeout(clientTimeoutProperties.getReadTimeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    throw exceptionFactory.fromHttpStatus(response.getStatusCode(), request.getURI());
                });
    }

    private <T> T createHttpProxy(RestClient restClient, Class<T> clientClass) {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(clientClass);
    }
}
