package backend.academy.linktracker.bot.service.mapper;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GrpcMapper {

    public LinkResponse fromProto(backend.academy.linktracker.grpc.LinkResponse proto) {
        LinkResponse response = new LinkResponse();
        response.setId(proto.getId());
        response.setUrl(URI.create(proto.getUrl()));
        response.setTags(proto.getTagsList());
        response.setFilters(proto.getFiltersList());

        return response;
    }

    public ListLinksResponse fromProto(backend.academy.linktracker.grpc.ListLinksResponse proto) {
        List<LinkResponse> links =
                proto.getLinksList().stream().map(this::fromProto).toList();

        ListLinksResponse response = new ListLinksResponse();
        response.setLinks(links);
        response.setSize(proto.getSize());

        return response;
    }
}
