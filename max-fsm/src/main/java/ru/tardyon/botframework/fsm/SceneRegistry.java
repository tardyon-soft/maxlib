package ru.tardyon.botframework.fsm;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry of known scene definitions.
 */
public interface SceneRegistry {

    /**
     * Registers scene definition by {@link Scene#id()}.
     */
    SceneRegistry register(Scene scene);

    /**
     * Finds scene by id.
     */
    Optional<Scene> find(String sceneId);

    /**
     * Returns immutable snapshot of all registered scenes.
     */
    Collection<Scene> all();
}
