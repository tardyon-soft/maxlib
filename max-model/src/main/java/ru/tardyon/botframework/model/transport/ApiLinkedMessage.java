package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * MAX API shape for link.message payload in Message.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiLinkedMessage(
        @JsonProperty("mid") String mid,
        @JsonProperty("seq") Long seq,
        @JsonProperty("text") String text,
        @JsonProperty("attachments") List<Object> attachments,
        @JsonProperty("markup") List<Object> markup
) {
    public ApiLinkedMessage {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
        markup = markup == null ? List.of() : List.copyOf(markup);
    }
}
