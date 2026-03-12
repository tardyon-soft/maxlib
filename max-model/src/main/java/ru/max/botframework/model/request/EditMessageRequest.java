package ru.max.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.MessageId;

/**
 * Request DTO for editing an existing message.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EditMessageRequest(
        ChatId chatId,
        MessageId messageId,
        NewMessageBody body,
        @JsonProperty("notify") Boolean sendNotification
) {
    public EditMessageRequest {
        Objects.requireNonNull(chatId, "chatId");
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(body, "body");
        sendNotification = sendNotification == null ? Boolean.TRUE : sendNotification;
    }
}
