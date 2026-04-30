package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

/**
 * Message media attachment DTO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageAttachment(
        MessageAttachmentType type,
        FileId fileId,
        String url,
        String mimeType,
        Long size,
        Object payload
) {
    public MessageAttachment {
        Objects.requireNonNull(type, "type");
        if (size != null && size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
    }

    public MessageAttachment(
            MessageAttachmentType type,
            FileId fileId,
            String url,
            String mimeType,
            Long size
    ) {
        this(type, fileId, url, mimeType, size, null);
    }
}
