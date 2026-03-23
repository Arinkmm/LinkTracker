package backend.academy.linktracker.scrapper.repository.jpa;

import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.jpa.entity.LinkEntity;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;

@RequiredArgsConstructor
public class LinkJpaAdapter implements LinkRepository {
    private final JpaLinkRepository jpaLinkRepository;

    @Override
    public Link save(URI url) {
        return jpaLinkRepository
                .findByUrl(url.toString())
                .map(this::toDto)
                .orElseGet(() -> toDto(jpaLinkRepository.saveAndFlush(LinkEntity.from(url))));
    }

    @Override
    public List<Link> findByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpaLinkRepository.findAllById(ids).stream().map(this::toDto).toList();
    }

    @Override
    public Optional<Link> findByUrl(URI url) {
        return jpaLinkRepository.findByUrl(url.toString()).map(this::toDto);
    }

    @Override
    public void remove(Long id) {
        jpaLinkRepository.deleteById(id);
    }

    @Override
    public List<Link> findStaleLinks(Instant threshold, int page, int size) {
        return jpaLinkRepository.findStaleLinks(threshold, PageRequest.of(page, size)).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public void updateLastChecked(Long id, Instant newTime) {
        jpaLinkRepository.findById(id).ifPresent(ormLinkEntity -> {
            ormLinkEntity.setLastChecked(newTime);
            jpaLinkRepository.save(ormLinkEntity);
        });
    }

    private Link toDto(LinkEntity entity) {
        return new Link(entity.getId(), URI.create(entity.getUrl()), entity.getLastChecked());
    }
}
