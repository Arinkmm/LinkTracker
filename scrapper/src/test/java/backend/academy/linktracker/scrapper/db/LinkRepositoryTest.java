package backend.academy.linktracker.scrapper.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.linktracker.scrapper.configuration.DatabaseIntegrationEnvironment;
import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class LinkRepositoryTest extends DatabaseIntegrationEnvironment {
    @Autowired
    private LinkRepository linkRepository;

    @Test
    @DisplayName("Миграции применились — БД доступна")
    void migrationsShouldApplySuccessfully() {
        assertTrue(postgres.isRunning());
    }

    @Test
    @DisplayName("Добавление и удаление ссылки")
    void shouldSaveAndRemoveLink() {
        URI url = URI.create("https://example.com");

        Link saved = linkRepository.save(url);
        assertThat(saved.id()).isNotNull();
        assertThat(linkRepository.findByUrl(url)).isPresent();

        linkRepository.remove(saved.id());
        assertThat(linkRepository.findByUrl(url)).isEmpty();
    }
}
