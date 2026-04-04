package backend.academy.linktracker.scrapper.configuration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ExternalApiIntegrationEnvironment extends DatabaseIntegrationEnvironment {

    @RegisterExtension
    protected static final WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @DynamicPropertySource
    static void overrideWebClientProperties(DynamicPropertyRegistry registry) {
        String wireMockUrl = wireMock.baseUrl();

        registry.add("app.github.url", () -> wireMockUrl);
        registry.add("app.stackoverflow.url", () -> wireMockUrl);
    }

    protected void stubGitHub(String owner, String repo, int status, String body) {
        wireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/repos/" + owner + "/" + repo + "/issues"))
            .willReturn(WireMock.aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
    }

    protected void stubStackOverflow(String ids, int status, String body) {
        wireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/questions/" + ids))
            .willReturn(WireMock.aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body)));
    }
}
