package ru.max.botframework.fsm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable payload associated with a state key.
 */
public record StateData(Map<String, Object> values) {

    public StateData {
        Objects.requireNonNull(values, "values");
        values = Map.copyOf(values);
    }

    public static StateData empty() {
        return new StateData(Map.of());
    }

    public static StateData of(Map<String, Object> values) {
        return new StateData(values);
    }

    public StateData merge(Map<String, Object> patch) {
        Objects.requireNonNull(patch, "patch");
        if (patch.isEmpty()) {
            return this;
        }

        Map<String, Object> merged = new LinkedHashMap<>(values);
        merged.putAll(patch);
        return new StateData(merged);
    }

    public Optional<Object> get(String key) {
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(values.get(key));
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        Objects.requireNonNull(type, "type");
        return get(key)
                .filter(type::isInstance)
                .map(type::cast);
    }
}
