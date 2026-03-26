package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * MAX API message body shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiMessageBody(
        @JsonProperty("mid") String mid,
        @JsonProperty("seq") Long seq,
        @JsonProperty("text") String text,
        @JsonProperty("attachments") List<Object> attachments,
        @JsonProperty("markup") List<Object> markup
) {
    public ApiMessageBody {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
        markup = markup == null ? List.of() : List.copyOf(markup);
    }
}
