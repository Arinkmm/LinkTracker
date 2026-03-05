package backend.academy.linktracker.scrapper.exception;

public class ChatNotFoundException extends RuntimeException {
    public ChatNotFoundException() {
        super("Чат не существует");
    }
}
