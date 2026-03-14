package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import backend.academy.linktracker.bot.exception.ApiException;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.grpc.ScrapperServiceGrpc;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannelBuilder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class HttpConfiguration {
    private final CommandProperties commandProperties;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "http", matchIfMissing = true)
    public RestClient scrapperRestClient(@Value("${app.scrapper.url}") String url, ObjectMapper objectMapper) {
        return RestClient.builder()
                .baseUrl(url)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    String rawBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    try {
                        ApiErrorResponse error = objectMapper.readValue(rawBody, ApiErrorResponse.class);
                        throw new ApiException(error);
                    } catch (Exception e) {
                        ApiErrorResponse fallbackError = new ApiErrorResponse();
                        fallbackError.setCode(
                                String.valueOf(response.getStatusCode().value()));
                        fallbackError.setDescription(
                                commandProperties.getMessages().getInvalidResponse());
                        fallbackError.setExceptionMessage(rawBody);

                        throw new ApiException(fallbackError);
                    }
                })
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "grpc")
    public ScrapperServiceGrpc.ScrapperServiceBlockingStub scrapperGrpcStub(
            @Value("${app.grpc.host}") String host, @Value("${app.grpc.port}") int port) {
        return ScrapperServiceGrpc.newBlockingStub(
                ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
    }
}
