package backend.academy.linktracker.bot.model;

public enum State {
    OK,
    NEEDED_TAGS,
    WAITING_TAGS,
    WAITING_URL,
    WAITING_UNTRACKING_URL;
}
