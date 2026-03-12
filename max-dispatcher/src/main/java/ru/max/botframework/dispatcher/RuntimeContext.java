package ru.max.botframework.dispatcher;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import ru.max.botframework.model.Update;

/**
 * Request-scoped runtime context used by middleware and future DI/resolution layers.
 */
public final class RuntimeContext {
    private final Update update;
    private final Map<ContextKey<?>, Object> attributes;

    public RuntimeContext(Update update) {
        this.update = Objects.requireNonNull(update, "update");
        this.attributes = new ConcurrentHashMap<>();
    }

    public Update update() {
        return update;
    }

    public <T> RuntimeContext put(ContextKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (!key.type().isInstance(value)) {
            throw new IllegalArgumentException("value type does not match context key type");
        }
        attributes.put(key, value);
        return this;
    }

    public <T> Optional<T> get(ContextKey<T> key) {
        Objects.requireNonNull(key, "key");
        Object raw = attributes.get(key);
        if (raw == null) {
            return Optional.empty();
        }
        if (!key.type().isInstance(raw)) {
            return Optional.empty();
        }
        return Optional.of(key.type().cast(raw));
    }

    public Map<ContextKey<?>, Object> attributes() {
        return Map.copyOf(attributes);
    }
}

