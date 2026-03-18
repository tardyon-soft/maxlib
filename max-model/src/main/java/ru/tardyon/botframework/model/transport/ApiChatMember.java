package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MAX API transport chat member shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiChatMember(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("chat_id") Long chatId,
        @JsonProperty("status") String status,
        @JsonProperty("user") ApiUser user
) {
}
