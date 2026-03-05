package backend.academy.linktracker.bot.model;

public enum InternalCommand {
    START("start"),
    HELP("help"),
    LIST("list"),
    TRACK("track"),
    UNTRACK("untrack"),
    CANCEL("cancel");

    public final String configKey;

    InternalCommand(String configKey) {
        this.configKey = configKey;
    }
}

