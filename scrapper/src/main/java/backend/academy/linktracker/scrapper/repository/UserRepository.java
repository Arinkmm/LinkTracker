package backend.academy.linktracker.scrapper.repository;

public interface UserRepository {
    void save(Long id);

    boolean exists(Long id);

    void delete(Long id);
}
