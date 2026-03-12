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
    private final Map<String, Object> enrichment;

    public RuntimeContext(Update update) {
        this.update = Objects.requireNonNull(update, "update");
        this.attributes = new ConcurrentHashMap<>();
        this.enrichment = new ConcurrentHashMap<>();
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

    public RuntimeContext putAllEnrichment(Map<String, Object> values) {
        Objects.requireNonNull(values, "values");
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("enrichment key must not be blank");
            }
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("enrichment value must not be null");
            }
            enrichment.put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public Optional<Object> enrichmentValue(String key) {
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(enrichment.get(key));
    }

    public Map<String, Object> enrichment() {
        return Map.copyOf(enrichment);
    }
}
