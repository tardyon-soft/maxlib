package ru.max.botframework.dispatcher;

import java.util.Objects;

/**
 * Typed key for request-scoped runtime data container.
 *
 * @param <T> value type
 */
public record RuntimeDataKey<T>(String name, Class<T> type, RuntimeDataScope scope) {
    public RuntimeDataKey {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(scope, "scope");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    public static <T> RuntimeDataKey<T> framework(String name, Class<T> type) {
        return new RuntimeDataKey<>(name, type, RuntimeDataScope.FRAMEWORK);
    }

    public static <T> RuntimeDataKey<T> filter(String name, Class<T> type) {
        return new RuntimeDataKey<>(name, type, RuntimeDataScope.FILTER);
    }

    public static <T> RuntimeDataKey<T> middleware(String name, Class<T> type) {
        return new RuntimeDataKey<>(name, type, RuntimeDataScope.MIDDLEWARE);
    }

    public static <T> RuntimeDataKey<T> application(String name, Class<T> type) {
        return new RuntimeDataKey<>(name, type, RuntimeDataScope.APPLICATION);
    }
}
