package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

/**
 * Type-safe uploaded file identifier.
 */
public record FileId(String value) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public FileId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    @JsonValue
    public String value() {
        return value;
    }
}
