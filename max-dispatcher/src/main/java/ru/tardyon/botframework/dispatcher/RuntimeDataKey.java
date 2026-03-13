package ru.tardyon.botframework.dispatcher;

import java.util.Objects;

/**
 * Typed key for request-scoped runtime data container.
 *
 * <p>Keys are identified by {@code name + scope}. For deterministic behavior
 * keep names stable and unique within one logical source.</p>
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

    /**
     * Filter-produced data key.
     */
    public static <T> RuntimeDataKey<T> filter(String name, Class<T> type) {
        return new RuntimeDataKey<>(name, type, RuntimeDataScope.FILTER);
    }

    /**
     * Middleware-produced data key.
     */
    public static <T> RuntimeDataKey<T> middleware(String name, Class<T> type) {
        return new RuntimeDataKey<>(name, type, RuntimeDataScope.MIDDLEWARE);
    }

    /**
     * Application/service data key registered on dispatcher.
     */
    public static <T> RuntimeDataKey<T> application(String name, Class<T> type) {
        return new RuntimeDataKey<>(name, type, RuntimeDataScope.APPLICATION);
    }
}
