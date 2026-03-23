package backend.academy.linktracker.scrapper.repository.jpa;

import backend.academy.linktracker.scrapper.repository.UserRepository;
import backend.academy.linktracker.scrapper.repository.jpa.entity.UserEntity;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserJpaAdapter implements UserRepository {
    private final JpaUserRepository jpaUserRepository;

    @Override
    public void save(Long id) {
        jpaUserRepository.save(new UserEntity(id));
    }

    @Override
    public boolean exists(Long id) {
        return jpaUserRepository.existsById(id);
    }

    @Override
    public void delete(Long id) {
        jpaUserRepository.deleteById(id);
    }
}
