package ru.tardyon.botframework.fsm;

import java.time.Instant;
import java.util.Objects;

/**
 * Stored scene metadata for one FSM scope.
 */
public record SceneSession(String sceneId, Instant enteredAt) {
    public SceneSession {
        Objects.requireNonNull(sceneId, "sceneId");
        Objects.requireNonNull(enteredAt, "enteredAt");
        if (sceneId.isBlank()) {
            throw new IllegalArgumentException("sceneId must not be blank");
        }
    }
}
