package ru.tardyon.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Docs-shaped request for PUT /chats/{chatId}/pin.
 */
public record PinChatMessageApiRequest(
        @JsonProperty("message_id") String messageId,
        @JsonProperty("notify") Boolean notifyValue
) {
}
