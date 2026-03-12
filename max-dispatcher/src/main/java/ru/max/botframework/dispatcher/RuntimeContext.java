package ru.max.botframework.dispatcher;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import ru.max.botframework.model.Update;

/**
 * Request-scoped runtime context used by middleware and future DI/resolution layers.
 *
 * <p>Context exposes:
 * request attributes (`ContextKey`) and typed runtime data container (`RuntimeDataContainer`).
 * Runtime data keeps framework/filter/middleware/application scopes isolated per update lifecycle.</p>
 */
public final class RuntimeContext {
    private static final String CONTEXT_KEY_PREFIX = "ctx.";
    private final Update update;
    private final Map<ContextKey<?>, Object> attributes;
    private final RuntimeDataContainer data;

    public RuntimeContext(Update update) {
        this.update = Objects.requireNonNull(update, "update");
        this.attributes = new ConcurrentHashMap<>();
        this.data = new RuntimeDataContainer();
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
        data.put(RuntimeDataKey.framework(contextKeyName(key), key.type()), value);
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

    public RuntimeDataContainer data() {
        return data;
    }

    public <T> RuntimeContext putData(RuntimeDataKey<T> key, T value) {
        data.put(key, value);
        return this;
    }

    public <T> RuntimeContext replaceData(RuntimeDataKey<T> key, T value) {
        data.replace(key, value);
        return this;
    }

    public <T> Optional<T> dataValue(RuntimeDataKey<T> key) {
        return data.get(key);
    }

    public <T> RuntimeContext putEnrichment(ContextKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (!key.type().isInstance(value)) {
            throw new IllegalArgumentException("value type does not match context key type");
        }
        putEnrichmentValue(key.name(), value, RuntimeDataScope.MIDDLEWARE, "context key");
        return this;
    }

    public RuntimeContext putEnrichment(String key, Object value) {
        putEnrichmentValue(key, value, RuntimeDataScope.MIDDLEWARE, "middleware");
        return this;
    }

    /**
     * @deprecated Prefer {@link #putEnrichment(String, Object)} for middleware and internal filter merge path.
     */
    @Deprecated(forRemoval = false)
    public RuntimeContext putAllEnrichment(Map<String, Object> values) {
        return mergeFilterEnrichment(values);
    }

    /**
     * Framework-internal filter enrichment merge.
     *
     * <p>Conflict policy: if the same key already exists with a different value,
     * {@link EnrichmentConflictException} is thrown.</p>
     */
    RuntimeContext mergeFilterEnrichment(Map<String, Object> values) {
        Objects.requireNonNull(values, "values");
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            putEnrichmentValue(entry.getKey(), entry.getValue(), RuntimeDataScope.FILTER, "filter");
        }
        return this;
    }

    public <T> Optional<T> enrichmentValue(ContextKey<T> key) {
        Objects.requireNonNull(key, "key");
        return enrichmentValue(key.name(), key.type());
    }

    public <T> Optional<T> enrichmentValue(String key, Class<T> type) {
        return data.find(key, Objects.requireNonNull(type, "type"));
    }

    public Optional<Object> enrichmentValue(String key) {
        return data.find(Objects.requireNonNull(key, "key"));
    }

    public Map<String, Object> enrichment() {
        return data.snapshot(RuntimeDataScope.FILTER, RuntimeDataScope.MIDDLEWARE);
    }

    private void putEnrichmentValue(String key, Object value, RuntimeDataScope scope, String source) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (key.isBlank()) {
            throw new IllegalArgumentException("enrichment key must not be blank");
        }
        try {
            data.put(new RuntimeDataKey<>(key, Object.class, scope), value);
        } catch (RuntimeDataConflictException conflict) {
            throw EnrichmentConflictException.conflict(
                    conflict.keyName(),
                    conflict.existingValue(),
                    conflict.incomingValue(),
                    source
            );
        }
    }

    private static <T> String contextKeyName(ContextKey<T> key) {
        return CONTEXT_KEY_PREFIX + key.name();
    }
}
