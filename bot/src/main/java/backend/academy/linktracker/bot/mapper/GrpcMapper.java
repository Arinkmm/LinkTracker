package backend.academy.linktracker.bot.mapper;

import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class GrpcMapper {
    public backend.academy.linktracker.bot.dto.LinkResponse fromProto(backend.academy.linktracker.grpc.LinkResponse proto) {
        return new backend.academy.linktracker.bot.dto.LinkResponse(
            proto.getId(),
            proto.getUrl(),
            proto.getTagsList(),
            proto.getFiltersList()
        );
    }

    public ListLinksResponse fromProto(backend.academy.linktracker.grpc.ListLinksResponse proto) {
        List<LinkResponse> links = proto.getLinksList().stream()
            .map(this::fromProto)
            .toList();
        return new ListLinksResponse(links, proto.getSize());
    }
}
