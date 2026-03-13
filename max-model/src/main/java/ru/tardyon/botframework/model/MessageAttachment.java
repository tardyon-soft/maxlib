package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * Message media attachment DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageAttachment(
        MessageAttachmentType type,
        FileId fileId,
        String url,
        String mimeType,
        Long size
) {
    public MessageAttachment {
        Objects.requireNonNull(type, "type");
        if (size != null && size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
    }
}
