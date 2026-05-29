package backend.academy.linktracker.bot.metrics;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "management.metrics.pushgateway", name = "enabled", havingValue = "true")
@Slf4j
public class PrometheusPushGatewayPublisher {
    private final PrometheusMeterRegistry meterRegistry;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String pushGatewayUrl;
    private final String job;
    private final String appType;

    public PrometheusPushGatewayPublisher(
            PrometheusMeterRegistry meterRegistry,
            @Value("${management.metrics.pushgateway.url}") String pushGatewayUrl,
            @Value("${management.metrics.pushgateway.job}") String job,
            @Value("${management.metrics.pushgateway.app-type}") String appType) {
        this.meterRegistry = meterRegistry;
        this.pushGatewayUrl = pushGatewayUrl;
        this.job = job;
        this.appType = appType;
    }

    @Scheduled(fixedDelayString = "${management.metrics.pushgateway.interval-ms}")
    public void publish() {
        try {
            HttpResponse<Void> response = httpClient.send(request(), HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 400) {
                log.atWarn().addKeyValue("status", response.statusCode()).log("Failed to push metrics to Pushgateway");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.atWarn().setCause(e).log("Metrics push to Pushgateway was interrupted");
        } catch (IOException | IllegalArgumentException e) {
            log.atWarn().setCause(e).log("Failed to push metrics to Pushgateway");
        }
    }

    private HttpRequest request() {
        return HttpRequest.newBuilder(pushGatewayUri())
                .header("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
                .PUT(HttpRequest.BodyPublishers.ofString(meterRegistry.scrape(), StandardCharsets.UTF_8))
                .build();
    }

    private URI pushGatewayUri() {
        return URI.create(
                trimTrailingSlash(pushGatewayUrl) + "/metrics/job/" + encode(job) + "/app_type/" + encode(appType));
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
