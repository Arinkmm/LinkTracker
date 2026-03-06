package backend.academy.linktracker.bot.service.bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramSender {
    private final TelegramBot bot;

    public void sendMessage(Long id, String text) {
        SendResponse response = bot.execute(new SendMessage(id, text));
        if (!response.isOk()) {
            log.atError()
                    .addKeyValue("error_code", response.errorCode())
                    .addKeyValue("description", response.description())
                    .log("Failed to send message");
        }
    }
}
