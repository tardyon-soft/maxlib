package ru.max.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * Request DTO for sending a new message.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SendMessageRequest(
        String chatId,
        NewMessageBody body,
        @JsonProperty("notify") Boolean sendNotification,
        String replyToMessageId
) {
    public SendMessageRequest {
        Objects.requireNonNull(chatId, "chatId");
        Objects.requireNonNull(body, "body");
        sendNotification = sendNotification == null ? Boolean.TRUE : sendNotification;
    }
}
