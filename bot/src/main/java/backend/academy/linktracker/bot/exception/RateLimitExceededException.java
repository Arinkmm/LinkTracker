package backend.academy.linktracker.bot.exception;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String ip, String path) {
        super("Лимит запросов превышен для ip=" + ip + ", path=" + path);
    }
}
