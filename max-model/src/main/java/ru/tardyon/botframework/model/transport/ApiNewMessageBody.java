package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import ru.tardyon.botframework.model.TextFormat;
import ru.tardyon.botframework.model.request.NewMessageAttachment;

/**
 * MAX API transport request body for send/edit message.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiNewMessageBody(
        @JsonProperty("text") String text,
        @JsonProperty("attachments") List<NewMessageAttachment> attachments,
        @JsonProperty("link") ApiNewMessageLink link,
        @JsonProperty("notify") Boolean notifyValue,
        @JsonProperty("format") TextFormat format
) {
    public ApiNewMessageBody {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
}
