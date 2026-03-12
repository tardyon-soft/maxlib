package ru.max.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * Request DTO for editing an existing message.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EditMessageRequest(
        String chatId,
        String messageId,
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
