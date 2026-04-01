package backend.academy.linktracker.bot.commands;

import com.pengrad.telegrambot.model.Message;

public interface Command {
    String command();

    void handle(Message message);
}
