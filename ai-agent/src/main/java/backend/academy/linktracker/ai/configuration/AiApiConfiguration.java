package backend.academy.linktracker.ai.configuration;

import backend.academy.linktracker.ai.client.api.AiApiClient;
import backend.academy.linktracker.ai.exception.AiSummarizationException;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@RequiredArgsConstructor
public class AiApiConfiguration {
    private final AiAgentProperties properties;

    @Bean
    public AiApiClient aiApiClient() {
        AiAgentProperties.Api api = properties.getSummarization().getApi();
        RestClient restClient = baseRestClientBuilder()
                .baseUrl(api.getUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + api.getToken())
                .build();

        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(AiApiClient.class);
    }

    private RestClient.Builder baseRestClientBuilder() {
        AiAgentProperties.Api api = properties.getSummarization().getApi();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(api.getTimeout()))
                .setConnectionRequestTimeout(Timeout.of(api.getTimeout()))
                .setResponseTimeout(Timeout.of(api.getTimeout()))
                .build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                HttpClients.custom().setDefaultRequestConfig(requestConfig).build());
        requestFactory.setReadTimeout(api.getTimeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    String rawBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    throw new AiSummarizationException("AI API returned " + response.getStatusCode() + ": " + rawBody);
                });
    }
}
