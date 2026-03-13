package ru.tardyon.botframework.fsm;

import java.util.Objects;

/**
 * Strategy that maps scene id to underlying FSM state id.
 */
@FunctionalInterface
public interface SceneStateBinding {

    String stateFor(String sceneId);

    static SceneStateBinding prefixed(String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        return sceneId -> {
            Objects.requireNonNull(sceneId, "sceneId");
            if (sceneId.isBlank()) {
                throw new IllegalArgumentException("sceneId must not be blank");
            }
            return prefix + sceneId;
        };
    }
}
