package ru.tardyon.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import ru.tardyon.botframework.model.TextFormat;

/**
 * Body payload for send/edit message requests.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NewMessageBody(
        String text,
        TextFormat format,
        List<NewMessageAttachment> attachments
) {
    public NewMessageBody {
        format = format == null ? TextFormat.PLAIN : format;
        attachments = attachments == null ? List.of() : List.copyOf(attachments);

        if ((text == null || text.isBlank()) && attachments.isEmpty()) {
            throw new IllegalArgumentException("Message body must contain text or attachments");
        }
    }
}
