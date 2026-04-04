package backend.academy.linktracker.bot.service.bot;

import backend.academy.linktracker.bot.properties.SenderProperties;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramSender {
    private final TelegramBot bot;
    private final SenderProperties senderProperties;

    public void sendMessage(Long id, String text) {
        if (text == null || text.isEmpty()) return;

        if (text.length() <= senderProperties.getMessageLimit()) {
            executeMessage(id, text);
            return;
        }

        log.info("Message for chat {} is too long ({} chars). Splitting...", id, text.length());

        List<String> chunks = splitMessage(text);
        for (int i = 0; i < chunks.size(); i++) {
            String part = chunks.get(i);
            if (i < chunks.size() - 1) {
                part += "\n\n" + senderProperties.getContinuer();
            }
            executeMessage(id, part);
        }
    }

    private List<String> splitMessage(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + senderProperties.getMessageLimit(), text.length());

            if (end < text.length()) {
                int lastSeparator = text.lastIndexOf(senderProperties.getDelimiter(), end);

                if (lastSeparator > start) {
                    end = lastSeparator + senderProperties.getDelimiter().length();
                } else {
                    int lastNewLine = text.lastIndexOf('\n', end);
                    if (lastNewLine > start) {
                        end = lastNewLine;
                    }
                }
            }
            chunks.add(text.substring(start, end).trim());
            start = end;
        }
        return chunks;
    }

    private void executeMessage(Long id, String text) {
        SendResponse response = bot.execute(new SendMessage(id, text));
        if (!response.isOk()) {
            log.atError()
                    .addKeyValue("error_code", response.errorCode())
                    .addKeyValue("description", response.description())
                    .log("Failed to send message");
        }
    }
}
