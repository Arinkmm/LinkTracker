package backend.academy.linktracker.bot.model;

public class User {
    private Long chatId;

    public User() {}

    public User(Long chatId) {
        this.chatId = chatId;
    }

    public Long getChatId() {
        return chatId;
    }
}
