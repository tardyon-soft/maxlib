package ru.tardyon.botframework.dispatcher;

import java.util.Objects;

/**
 * Typed key for request-scoped runtime context enrichment.
 *
 * @param <T> value type
 */
public record ContextKey<T>(String name, Class<T> type) {
    public ContextKey {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    public static <T> ContextKey<T> of(String name, Class<T> type) {
        return new ContextKey<>(name, type);
    }
}

