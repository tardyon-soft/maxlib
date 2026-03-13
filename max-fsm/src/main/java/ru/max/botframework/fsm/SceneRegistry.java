package ru.max.botframework.fsm;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry of known scene definitions.
 */
public interface SceneRegistry {

    SceneRegistry register(Scene scene);

    Optional<Scene> find(String sceneId);

    Collection<Scene> all();
}
