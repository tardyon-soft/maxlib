package ru.max.botframework.upload;

import java.util.Objects;

/**
 * Immutable uploaded object reference used by message attachment mapping.
 */
public record UploadRef(String value) {
    public UploadRef {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }
}
