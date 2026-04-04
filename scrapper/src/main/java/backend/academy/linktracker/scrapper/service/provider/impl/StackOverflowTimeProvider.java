package backend.academy.linktracker.scrapper.service.provider.impl;

import backend.academy.linktracker.scrapper.client.api.LinkResponse;
import backend.academy.linktracker.scrapper.client.api.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.client.api.stackoverflow.StackOverflowResponse;
import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import backend.academy.linktracker.scrapper.service.provider.LinkTimeProvider;
import com.google.common.collect.Lists;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StackOverflowTimeProvider implements LinkTimeProvider {
    private final StackOverflowClient client;
    private final StackoverflowProperties properties;

    private StackOverflowResponse fetchFromApi(List<String> ids) {
        log.atDebug()
                .addKeyValue("idsCount", ids.size())
                .addKeyValue("site", properties.getSite())
                .log("Calling StackOverflow API");

        return client.getQuestions(String.join(";", ids), properties.getSite(), properties.getFilter());
    }

    @Override
    public boolean supports(URI url) {
        return url.getHost() != null
                && url.getHost().contains("stackoverflow.com")
                && url.getPath() != null
                && url.getPath().contains("/questions/");
    }

    @Override
    public List<ResponseWithLink> getResponseBatch(List<Link> links) {
        log.atInfo().addKeyValue("totalLinks", links.size()).log("Processing StackOverflow batch");
        List<ResponseWithLink> results = new ArrayList<>();

        Lists.partition(links, properties.getBatchSize()).forEach(batch -> {
            Map<String, Link> idToLink =
                    batch.stream().collect(Collectors.toMap(l -> extractQuestionId(l.url()), l -> l, (a, b) -> a));

            List<String> ids = new ArrayList<>(idToLink.keySet());
            try {
                StackOverflowResponse response = fetchFromApi(ids);

                if (response == null || response.items() == null) {
                    log.atWarn().addKeyValue("ids", ids).log("API returned empty response for batch");
                    batch.forEach(l -> results.add(new ResponseWithLink(l, new StackOverflowResponse(List.of()))));
                    return;
                }

                Map<String, StackOverflowResponse.Item> itemsById = response.items().stream()
                        .collect(Collectors.toMap(i -> String.valueOf(i.questionId()), i -> i));

                for (Link link : batch) {
                    String id = extractQuestionId(link.url());
                    StackOverflowResponse.Item rawItem = itemsById.get(id);

                    if (rawItem == null) {
                        log.atTrace().addKeyValue("id", id).log("Question not found in API response");
                    }

                    StackOverflowResponse filteredResponse = filterItemActivity(rawItem, link.lastChecked());

                    if (!filteredResponse.items().isEmpty()) {
                        log.atDebug().addKeyValue("id", id).log("Found new activity for StackOverflow question");
                    }

                    results.add(new ResponseWithLink(link, filteredResponse));
                }

            } catch (Exception e) {
                log.atError().setCause(e).addKeyValue("ids", ids).log("Failed to process StackOverflow batch");
            }
        });
        return results;
    }

    @Override
    public LinkResponse getResponse(Link link) {
        String id = extractQuestionId(link.url());
        log.atDebug().addKeyValue("id", id).log("Fetching single StackOverflow question");

        try {
            StackOverflowResponse response = fetchFromApi(List.of(id));
            StackOverflowResponse.Item item =
                    (response != null && !response.items().isEmpty())
                            ? response.items().getFirst()
                            : null;

            return filterItemActivity(item, link.lastChecked());
        } catch (Exception e) {
            log.atError().setCause(e).addKeyValue("id", id).log("Error fetching single question");
            return new StackOverflowResponse(List.of());
        }
    }

    private StackOverflowResponse filterItemActivity(StackOverflowResponse.Item item, Instant lastChecked) {
        if (item == null) return new StackOverflowResponse(List.of());

        Instant threshold = (lastChecked == null) ? Instant.EPOCH : lastChecked;

        List<StackOverflowResponse.Answer> newAnswers = Optional.ofNullable(item.answers()).orElse(List.of()).stream()
                .filter(a -> Instant.ofEpochSecond(a.creationDate()).isAfter(threshold))
                .toList();

        List<StackOverflowResponse.Comment> newComments =
                Optional.ofNullable(item.comments()).orElse(List.of()).stream()
                        .filter(c -> Instant.ofEpochSecond(c.creationDate()).isAfter(threshold))
                        .toList();

        if (!newAnswers.isEmpty() || !newComments.isEmpty()) {
            return new StackOverflowResponse(List.of(new StackOverflowResponse.Item(
                    item.questionId(), item.body(), newAnswers, newComments, item.lastActivityDate())));
        }
        return new StackOverflowResponse(List.of());
    }

    private String extractQuestionId(URI url) {
        String path = url.getPath();
        if (path == null) {
            log.atTrace().addKeyValue("url", url).log("Could not extract ID: path is null");
            return null;
        }
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("questions".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        log.atTrace().addKeyValue("path", path).log("Could not find 'questions' segment in URL");
        return null;
    }
}
