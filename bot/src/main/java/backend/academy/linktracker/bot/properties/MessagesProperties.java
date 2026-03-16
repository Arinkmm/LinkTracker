package backend.academy.linktracker.bot.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "bot.messages")
public class MessagesProperties {
    private String welcome;
    private String helpHeader;
    private String unknownCommand;
    private String tracking;
    private String untracking;
    private String list;
    private String canceling;
    private String tagsOffer;
    private String invalidUrl;
    private String tagsPrompt;
    private String invalidTagsAnswer;
    private String links;
    private String linkAdded;
    private String linkAddedWithTags;
    private String linkDeleted;
    private String linkNotFound;
    private String linkIsEmpty;
    private String stateError;
    private String invalidResponse;
}
