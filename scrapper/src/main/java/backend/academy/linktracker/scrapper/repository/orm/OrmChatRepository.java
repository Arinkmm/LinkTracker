package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.ChatEntity;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrmChatRepository implements ChatRepository {
    private final JpaChatRepository jpaChatRepository;

    @Override
    public void save(Long id) {
        jpaChatRepository.save(new ChatEntity(id));
    }

    @Override
    public boolean exists(Long id) {
        return jpaChatRepository.existsById(id);
    }

    @Override
    public void delete(Long id) {
        jpaChatRepository.deleteById(id);
    }
}
