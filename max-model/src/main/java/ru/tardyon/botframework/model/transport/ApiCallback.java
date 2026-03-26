package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MAX API transport callback shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiCallback(
        @JsonProperty("callback_id") String callbackId,
        @JsonProperty("payload") @JsonAlias({"data", "callback_data"}) String payload,
        @JsonProperty("sender") @JsonAlias({"user", "from"}) ApiUser sender,
        @JsonProperty("message") ApiMessage message,
        @JsonProperty("message_id") String messageId,
        @JsonProperty("chat_id") String chatId,
        @JsonProperty("timestamp") Long timestamp
) {
}
