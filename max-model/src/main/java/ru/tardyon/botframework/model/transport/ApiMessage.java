package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MAX API transport message shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiMessage(
        @JsonProperty("message_id") @JsonAlias("mid") String messageId,
        @JsonProperty("sender") ApiUser sender,
        @JsonProperty("recipient") ApiRecipient recipient,
        @JsonProperty("timestamp") Long timestamp,
        @JsonProperty("link") ApiMessageLink link,
        @JsonProperty("body") ApiMessageBody body,
        @JsonProperty("stat") Object stat,
        @JsonProperty("url") String url
) {
}
