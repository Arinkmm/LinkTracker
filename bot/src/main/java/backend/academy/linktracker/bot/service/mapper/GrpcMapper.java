package backend.academy.linktracker.bot.service.mapper;

import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GrpcMapper {
    public LinkResponse fromProto(backend.academy.linktracker.grpc.LinkResponse proto) {
        return new LinkResponse(proto.getId(), proto.getUrl(), proto.getTagsList(), proto.getFiltersList());
    }

    public ListLinksResponse fromProto(backend.academy.linktracker.grpc.ListLinksResponse proto) {
        List<LinkResponse> links =
                proto.getLinksList().stream().map(this::fromProto).toList();
        return new ListLinksResponse(links, proto.getSize());
    }
}
