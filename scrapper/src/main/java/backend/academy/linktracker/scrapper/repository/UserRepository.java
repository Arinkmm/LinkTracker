package backend.academy.linktracker.scrapper.repository;


public interface UserRepository {
    void save(Long userId);

    boolean exists(Long userId);

    void delete(Long userId);
}
