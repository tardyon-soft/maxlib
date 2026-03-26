package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MAX API linked message reference shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiMessageLink(
        @JsonProperty("type") String type,
        @JsonProperty("sender") ApiUser sender,
        @JsonProperty("chat_id") Long chatId,
        @JsonProperty("message") ApiLinkedMessage message
) {
}
