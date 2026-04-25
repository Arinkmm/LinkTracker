package backend.academy.linktracker.scrapper.service.notifier.impl.kafka;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.exception.MessageSerializationException;
import backend.academy.linktracker.scrapper.repository.orm.entity.OutboxMessageEntity;
import backend.academy.linktracker.scrapper.service.notifier.BotNotifier;
import backend.academy.linktracker.scrapper.service.user.OutboxMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class KafkaBotNotifier implements BotNotifier {
    private final ObjectMapper objectMapper;
    private final OutboxMessageService outboxMessageService;

    @Override
    public void notify(LinkUpdate linkUpdate) {
        try {
            String payload = objectMapper.writeValueAsString(linkUpdate);
            OutboxMessageEntity message = new OutboxMessageEntity();
            message.setLinkId(linkUpdate.getId());
            message.setPayload(payload);
            log.atInfo()
                    .addKeyValue("id", linkUpdate.getId())
                    .addKeyValue("url", linkUpdate.getUrl())
                    .addKeyValue("chat_count", linkUpdate.getTgChatIds().size())
                    .log("Save message in outbox");
            outboxMessageService.addMessage(message);
        } catch (JsonProcessingException e) {
            log.atError().addKeyValue("linkId", linkUpdate.getId()).log("Error while saving message in serialization");
            throw new MessageSerializationException("Error serializing link update", e);
        }
    }
}
