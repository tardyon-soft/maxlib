package ru.tardyon.botframework.fsm;

/**
 * Raised when scene id is not present in registry.
 */
public final class SceneNotFoundException extends RuntimeException {

    public SceneNotFoundException(String sceneId) {
        super("Scene '%s' is not registered".formatted(sceneId));
    }
}
