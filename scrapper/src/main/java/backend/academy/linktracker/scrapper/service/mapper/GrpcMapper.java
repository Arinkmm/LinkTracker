package backend.academy.linktracker.scrapper.service.mapper;

import backend.academy.linktracker.grpc.AddLinkRequest;
import backend.academy.linktracker.grpc.LinkResponse;
import backend.academy.linktracker.grpc.ListLinksResponse;
import backend.academy.linktracker.grpc.RemoveLinkRequest;
import java.net.URI;
import org.springframework.stereotype.Component;

@Component
public class GrpcMapper {
    public backend.academy.linktracker.scrapper.dto.AddLinkRequest grpcToDto(AddLinkRequest grpc) {
        backend.academy.linktracker.scrapper.dto.AddLinkRequest dto =
                new backend.academy.linktracker.scrapper.dto.AddLinkRequest();

        dto.setLink(URI.create(grpc.getLink()));
        dto.setTags(grpc.getTagsList());

        return dto;
    }

    public backend.academy.linktracker.scrapper.dto.RemoveLinkRequest grpcToDto(RemoveLinkRequest grpc) {
        backend.academy.linktracker.scrapper.dto.RemoveLinkRequest dto =
                new backend.academy.linktracker.scrapper.dto.RemoveLinkRequest();

        dto.setLink(URI.create(grpc.getLink()));

        return dto;
    }

    public LinkResponse dtoToGrpc(backend.academy.linktracker.scrapper.dto.LinkResponse dto) {
        LinkResponse.Builder builder =
                LinkResponse.newBuilder().setId(dto.getId()).setUrl(dto.getUrl().toString());

        if (dto.getTags() != null) {
            builder.addAllTags(dto.getTags());
        }

        return builder.build();
    }

    public ListLinksResponse dtoToGrpc(backend.academy.linktracker.scrapper.dto.ListLinksResponse dto) {
        var builder = ListLinksResponse.newBuilder();

        if (dto.getLinks() != null) {
            builder.addAllLinks(dto.getLinks().stream().map(this::dtoToGrpc).toList());
            builder.setSize(dto.getLinks().size());
        }

        return builder.build();
    }
}
