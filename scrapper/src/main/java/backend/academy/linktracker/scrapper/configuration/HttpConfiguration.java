package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.grpc.BotServiceGrpc;
import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import backend.academy.linktracker.scrapper.exception.ApiException;
import backend.academy.linktracker.scrapper.properties.ErrorProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
    private final ErrorProperties errorProperties;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    public RestClient scrapperRestClient(@Value("${app.bot.url}") String url, ObjectMapper objectMapper) {
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
                        fallbackError.setDescription(errorProperties.getInvalidResponse());
                        fallbackError.setExceptionMessage(rawBody);

                        throw new ApiException(fallbackError);
                    }
                })
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "grpc")
    public BotServiceGrpc.BotServiceBlockingStub scrapperGrpcStub(
            @Value("${app.grpc.host}") String host, @Value("${app.grpc.port}") int port) {
        return BotServiceGrpc.newBlockingStub(
                ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
    }
}
