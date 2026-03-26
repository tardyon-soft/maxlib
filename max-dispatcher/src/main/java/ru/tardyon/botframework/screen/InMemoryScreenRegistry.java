package ru.tardyon.botframework.screen;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory screen registry.
 */
public final class InMemoryScreenRegistry implements ScreenRegistry {
    private final ConcurrentMap<String, ScreenDefinition> screens = new ConcurrentHashMap<>();

    @Override
    public Optional<ScreenDefinition> find(String screenId) {
        if (screenId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(screens.get(screenId));
    }

    @Override
    public ScreenRegistry register(ScreenDefinition definition) {
        ScreenDefinition nonNull = java.util.Objects.requireNonNull(definition, "definition");
        screens.put(nonNull.id(), nonNull);
        return this;
    }
}
