package ru.max.botframework.model;

import java.util.Objects;

/**
 * Normalized update coming from MAX transports (polling or webhook).
 */
public record Update(String updateId, String type) {
    public Update {
        Objects.requireNonNull(updateId, "updateId");
        Objects.requireNonNull(type, "type");
    }
}
