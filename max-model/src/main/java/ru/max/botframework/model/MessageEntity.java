package ru.max.botframework.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * Rich-text/entity segment inside message text.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageEntity(
        MessageEntityType type,
        int offset,
        int length,
        String value
) {
    public MessageEntity {
        Objects.requireNonNull(type, "type");
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException("offset and length must be non-negative");
        }
    }
}
