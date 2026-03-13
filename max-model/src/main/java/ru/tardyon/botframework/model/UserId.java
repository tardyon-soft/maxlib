package ru.tardyon.botframework.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

/**
 * Type-safe user identifier.
 */
public record UserId(String value) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public UserId {
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
