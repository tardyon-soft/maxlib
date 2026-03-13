package ru.max.botframework.fsm;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Storage for scene metadata/state binding, separate from FSM state payload storage.
 */
public interface SceneStorage {

    CompletionStage<Optional<SceneSession>> get(StateKey key);

    CompletionStage<Void> set(StateKey key, SceneSession session);

    CompletionStage<Void> clear(StateKey key);
}
