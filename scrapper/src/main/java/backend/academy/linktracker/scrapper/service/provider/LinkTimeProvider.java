package backend.academy.linktracker.scrapper.service.provider;

import backend.academy.linktracker.scrapper.client.api.LinkResponse;
import backend.academy.linktracker.scrapper.dto.Link;
import java.net.URI;
import java.util.List;

public interface LinkTimeProvider {
    boolean supports(URI url);

    LinkResponse getResponse(Link link);

    default List<ResponseWithLink> getResponseBatch(List<Link> links) {
        return links.stream().map(r -> new ResponseWithLink(r, getResponse(r))).toList();
    }

    record ResponseWithLink(Link link, LinkResponse linkResponse) {}
}
