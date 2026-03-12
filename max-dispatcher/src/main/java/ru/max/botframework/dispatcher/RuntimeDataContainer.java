package ru.max.botframework.dispatcher;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Request-scoped typed container for runtime data used by invocation and parameter resolution.
 *
 * <p>Conflict policy: writing different value for the same logical key is rejected by
 * {@link RuntimeDataConflictException}. Use {@link #replace(RuntimeDataKey, Object)} only for
 * explicit override semantics.</p>
 */
public final class RuntimeDataContainer {
    private final ConcurrentHashMap<String, Entry> data = new ConcurrentHashMap<>();

    /**
     * Puts value preserving conflict-safety semantics.
     */
    public <T> RuntimeDataContainer put(RuntimeDataKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (!key.type().isInstance(value)) {
            throw new IllegalArgumentException("value type does not match runtime data key type");
        }
        data.compute(key.name(), (name, existing) -> {
            if (existing == null) {
                return Entry.of(value, key.scope());
            }
            if (!Objects.equals(existing.value(), value)) {
                throw new RuntimeDataConflictException(name, existing.value(), value, key.scope());
            }
            return existing.withScope(key.scope());
        });
        return this;
    }

    /**
     * Replaces existing value for key (or creates new one if absent).
     */
    public <T> RuntimeDataContainer replace(RuntimeDataKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (!key.type().isInstance(value)) {
            throw new IllegalArgumentException("value type does not match runtime data key type");
        }
        data.compute(key.name(), (name, existing) -> existing == null
                ? Entry.of(value, key.scope())
                : existing.replaced(value, key.scope()));
        return this;
    }

    /**
     * Returns value for exact key (name + scope + type).
     */
    public <T> Optional<T> get(RuntimeDataKey<T> key) {
        Objects.requireNonNull(key, "key");
        Entry entry = data.get(key.name());
        if (entry == null || !entry.scopes().contains(key.scope())) {
            return Optional.empty();
        }
        if (!key.type().isInstance(entry.value())) {
            throw new IllegalStateException("runtime data key '%s' contains incompatible value type".formatted(key.name()));
        }
        return Optional.of(key.type().cast(entry.value()));
    }

    /**
     * Finds value by logical key name across all scopes.
     */
    public Optional<Object> find(String name) {
        Objects.requireNonNull(name, "name");
        Entry entry = data.get(name);
        return entry == null ? Optional.empty() : Optional.of(entry.value());
    }

    /**
     * Finds and type-casts value by logical key name across all scopes.
     */
    public <T> Optional<T> find(String name, Class<T> type) {
        Objects.requireNonNull(type, "type");
        return find(name).map(raw -> {
            if (!type.isInstance(raw)) {
                throw new IllegalStateException("runtime data key '%s' contains value incompatible with %s"
                        .formatted(name, type.getSimpleName()));
            }
            return type.cast(raw);
        });
    }

    /**
     * Immutable snapshot filtered by scopes.
     *
     * <p>If no scopes provided, all scopes are included.</p>
     */
    public Map<String, Object> snapshot(RuntimeDataScope... scopes) {
        Objects.requireNonNull(scopes, "scopes");
        EnumSet<RuntimeDataScope> expected = EnumSet.noneOf(RuntimeDataScope.class);
        if (scopes.length == 0) {
            expected.addAll(EnumSet.allOf(RuntimeDataScope.class));
        } else {
            for (RuntimeDataScope scope : scopes) {
                expected.add(Objects.requireNonNull(scope, "scope"));
            }
        }
        LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, Entry> entry : data.entrySet()) {
            if (entry.getValue().hasAnyScope(expected)) {
                snapshot.put(entry.getKey(), entry.getValue().value());
            }
        }
        return Map.copyOf(snapshot);
    }

    private record Entry(Object value, EnumSet<RuntimeDataScope> scopes) {
        private Entry {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(scopes, "scopes");
        }

        static Entry of(Object value, RuntimeDataScope scope) {
            return new Entry(value, EnumSet.of(scope));
        }

        Entry withScope(RuntimeDataScope scope) {
            EnumSet<RuntimeDataScope> merged = EnumSet.copyOf(scopes);
            merged.add(scope);
            return new Entry(value, merged);
        }

        Entry replaced(Object replaced, RuntimeDataScope scope) {
            EnumSet<RuntimeDataScope> merged = EnumSet.copyOf(scopes);
            merged.add(scope);
            return new Entry(replaced, merged);
        }

        boolean hasAnyScope(EnumSet<RuntimeDataScope> expected) {
            for (RuntimeDataScope scope : scopes) {
                if (expected.contains(scope)) {
                    return true;
                }
            }
            return false;
        }
    }
}
