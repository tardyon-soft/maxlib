package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MAX API message recipient shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiRecipient(
        @JsonProperty("chat_id") Long chatId,
        @JsonProperty("user_id") Long userId,
        @JsonProperty("chat_type") String chatType
) {
}
