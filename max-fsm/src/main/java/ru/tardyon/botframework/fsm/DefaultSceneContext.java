package ru.tardyon.botframework.fsm;

import java.util.Objects;

/**
 * Default immutable scene context.
 */
public record DefaultSceneContext(FSMContext fsm, SceneSession session) implements SceneContext {
    public DefaultSceneContext {
        Objects.requireNonNull(fsm, "fsm");
        Objects.requireNonNull(session, "session");
    }
}
