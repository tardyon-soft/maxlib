package ru.tardyon.botframework.fsm;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Storage for scene metadata/state binding, separate from FSM state payload storage.
 */
public interface SceneStorage {

    /**
     * Reads scene session bound to state key.
     */
    CompletionStage<Optional<SceneSession>> get(StateKey key);

    /**
     * Stores scene session for state key.
     */
    CompletionStage<Void> set(StateKey key, SceneSession session);

    /**
     * Clears scene session for state key.
     */
    CompletionStage<Void> clear(StateKey key);
}
