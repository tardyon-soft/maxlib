package ru.tardyon.botframework.screen;

import java.util.Optional;

/**
 * Registry of available screen definitions.
 */
public interface ScreenRegistry {
    Optional<ScreenDefinition> find(String screenId);

    ScreenRegistry register(ScreenDefinition definition);
}
