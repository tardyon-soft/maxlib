package ru.max.botframework.dispatcher;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import ru.max.botframework.model.Update;

/**
 * Request-scoped runtime context used by middleware and future DI/resolution layers.
 *
 * <p>Context has two namespaces:
 * typed attributes (`ContextKey`) and string-key enrichment map. Enrichment values are merged
 * from filters and middleware for one update lifecycle and never shared across updates.</p>
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

    public <T> RuntimeContext putEnrichment(ContextKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (!key.type().isInstance(value)) {
            throw new IllegalArgumentException("value type does not match context key type");
        }
        putEnrichmentValue(key.name(), value, "context key");
        return this;
    }

    public RuntimeContext putEnrichment(String key, Object value) {
        putEnrichmentValue(key, value, "middleware");
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
            putEnrichmentValue(entry.getKey(), entry.getValue(), "filter");
        }
        return this;
    }

    public <T> Optional<T> enrichmentValue(ContextKey<T> key) {
        Objects.requireNonNull(key, "key");
        return enrichmentValue(key.name(), key.type());
    }

    public <T> Optional<T> enrichmentValue(String key, Class<T> type) {
        Objects.requireNonNull(type, "type");
        return enrichmentValue(key).map(raw -> {
            if (!type.isInstance(raw)) {
                throw new IllegalStateException("enrichment key '%s' contains value incompatible with %s"
                        .formatted(key, type.getSimpleName()));
            }
            return type.cast(raw);
        });
    }

    public Optional<Object> enrichmentValue(String key) {
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(enrichment.get(key));
    }

    public Map<String, Object> enrichment() {
        return Map.copyOf(enrichment);
    }

    private void putEnrichmentValue(String key, Object value, String source) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (key.isBlank()) {
            throw new IllegalArgumentException("enrichment key must not be blank");
        }
        Object existing = enrichment.putIfAbsent(key, value);
        if (existing == null || Objects.equals(existing, value)) {
            return;
        }
        throw EnrichmentConflictException.conflict(key, existing, value, source);
    }
}
