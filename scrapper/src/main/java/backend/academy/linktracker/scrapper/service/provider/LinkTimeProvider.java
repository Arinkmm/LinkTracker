package backend.academy.linktracker.scrapper.service.provider;

import backend.academy.linktracker.scrapper.client.LinkResponse;
import backend.academy.linktracker.scrapper.dto.Link;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LinkTimeProvider {
    boolean supports(URI url);

    LinkResponse getResponse(Link link);

    default List<ResponseWithLink> getResponseBatch(List<Link> links) {
        return links.stream().map(r -> new ResponseWithLink(r, getResponse(r))).toList();
    }

    record ResponseWithLink(Link link, LinkResponse linkResponse) {}
}
