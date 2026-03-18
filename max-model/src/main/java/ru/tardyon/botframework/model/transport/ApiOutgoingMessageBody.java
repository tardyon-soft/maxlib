package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import ru.tardyon.botframework.model.TextFormat;

/**
 * Outgoing transport body for send/edit message requests.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiOutgoingMessageBody(
        @JsonProperty("text") String text,
        @JsonProperty("attachments") List<Object> attachments,
        @JsonProperty("link") ApiNewMessageLink link,
        @JsonProperty("notify") Boolean notifyValue,
        @JsonProperty("format") TextFormat format
) {
    public ApiOutgoingMessageBody {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
}
