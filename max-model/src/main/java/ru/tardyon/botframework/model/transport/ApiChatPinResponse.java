package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MAX API transport response for GET /chats/{chatId}/pin.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiChatPinResponse(
        @JsonProperty("message") ApiMessage message
) {
}
