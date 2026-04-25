package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import backend.academy.linktracker.bot.exception.ApiException;
import backend.academy.linktracker.bot.properties.MessagesProperties;
import backend.academy.linktracker.grpc.ScrapperServiceGrpc;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannelBuilder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ClientConfiguration {
    private final MessagesProperties properties;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnProperty(name = "app.scrapper-client.type", havingValue = "http", matchIfMissing = true)
    public RestClient scrapperRestClient(@Value("${app.scrapper.url}") String url, ObjectMapper objectMapper) {
        return RestClient.builder()
                .baseUrl(url)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    String rawBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    ApiErrorResponse error;
                    try {
                        error = objectMapper.readValue(rawBody, ApiErrorResponse.class);
                    } catch (Exception e) {
                        log.error("Failed to parse Scrapper error response: {}", rawBody, e);

                        error = new ApiErrorResponse();
                        error.setCode(String.valueOf(response.getStatusCode().value()));
                        error.setDescription(properties.getInvalidResponse());
                    }
                    throw new ApiException(error);
                })
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.scrapper-client.type", havingValue = "grpc")
    public ScrapperServiceGrpc.ScrapperServiceBlockingStub scrapperGrpcStub(
            @Value("${app.grpc.host}") String host, @Value("${app.grpc.port}") int port) {
        return ScrapperServiceGrpc.newBlockingStub(
                ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
    }
}
