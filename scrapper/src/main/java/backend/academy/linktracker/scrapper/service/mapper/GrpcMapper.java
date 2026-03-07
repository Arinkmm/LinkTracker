package backend.academy.linktracker.scrapper.service.mapper;

import backend.academy.linktracker.grpc.*;
import backend.academy.linktracker.grpc.AddLinkRequest;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class GrpcMapper {
    public backend.academy.linktracker.scrapper.dto.AddLinkRequest grpcToDto(AddLinkRequest grpc) {
        return new backend.academy.linktracker.scrapper.dto.AddLinkRequest(
                grpc.getLink(), grpc.getTagsList(), grpc.getFiltersList());
    }

    public backend.academy.linktracker.scrapper.dto.RemoveLinkRequest grpcToDto(RemoveLinkRequest grpc) {
        return new backend.academy.linktracker.scrapper.dto.RemoveLinkRequest(grpc.getLink());
    }

    public LinkResponse dtoToGrpc(backend.academy.linktracker.scrapper.dto.LinkResponse dto) {
        return LinkResponse.newBuilder()
                .setId(dto.id())
                .setUrl(dto.url())
                .addAllTags(dto.tags())
                .addAllFilters(dto.filters())
                .build();
    }

    public ListLinksResponse dtoToGrpc(backend.academy.linktracker.scrapper.dto.ListLinksResponse dto) {
        return ListLinksResponse.newBuilder()
                .addAllLinks(dto.links().stream().map(this::dtoToGrpc).collect(Collectors.toList()))
                .setSize(dto.size())
                .build();
    }
}
