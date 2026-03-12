package ru.max.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;
import ru.max.botframework.model.MessageAttachmentType;

/**
 * Minimal outgoing attachment DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NewMessageAttachment(
        MessageAttachmentType type,
        AttachmentInput input,
        String caption,
        String mimeType,
        Long size
) {
    public NewMessageAttachment {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(input, "input");
        if (size != null && size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
    }
}
