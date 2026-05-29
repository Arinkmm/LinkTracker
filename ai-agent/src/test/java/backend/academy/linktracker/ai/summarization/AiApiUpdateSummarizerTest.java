package backend.academy.linktracker.ai.summarization;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.ai.client.api.AiApiClient;
import backend.academy.linktracker.ai.configuration.AiApiConfiguration;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class AiApiUpdateSummarizerTest {
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    @DisplayName("AI API: суммаризация выполняется HTTP-вызовом к Hugging Face-compatible API")
    void shouldSummarizeViaAiApi() {
        wireMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer test-token"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("test-model")))
                .withRequestBody(matchingJsonPath("$.stream", equalTo("false")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "choices": [
                                    {
                                      "message": {
                                        "role": "assistant",
                                        "content": "Short AI summary"
                                      }
                                    }
                                  ]
                                }
                                """)));

        AiAgentProperties properties = properties(wireMock.baseUrl() + "/v1/chat/completions");
        AiApiClient aiApiClient = new AiApiConfiguration(properties).aiApiClient();
        AiApiUpdateSummarizer summarizer = new AiApiUpdateSummarizer(aiApiClient, properties);

        String result = summarizer.summarize("Very long update text");

        assertThat(result).isEqualTo("Short AI summary");
    }

    private static AiAgentProperties properties(String apiUrl) {
        AiAgentProperties.Api api =
                new AiAgentProperties.Api(apiUrl, "test-token", "test-model", Duration.ofSeconds(5), "Summarize");
        AiAgentProperties.Summarization summ = new AiAgentProperties.Summarization(10, api);
        AiAgentProperties.Filtering filt = new AiAgentProperties.Filtering(List.of(), List.of(), 0);
        AiAgentProperties.Labels labels = new AiAgentProperties.Labels("Описание: ", "────────────────");
        AiAgentProperties.Prioritization prioritization =
                new AiAgentProperties.Prioritization(List.of("critical"), List.of("typo"));
        AiAgentProperties.Grouping grouping = new AiAgentProperties.Grouping(100, 10);
        return new AiAgentProperties(filt, summ, labels, prioritization, grouping);
    }
}
