package ru.tardyon.botframework.fsm;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Async storage contract for FSM state and state payload.
 *
 * <p>The contract keeps state id and payload data as separate channels.
 * Implementations are intentionally minimal and do not imply transactions.</p>
 */
public interface FSMStorage {

    /**
     * Returns current state id or empty when state is not set.
     */
    CompletionStage<Optional<String>> getState(StateKey key);

    /**
     * Replaces current state id for key.
     */
    CompletionStage<Void> setState(StateKey key, String state);

    /**
     * Clears current state id while preserving payload data.
     */
    CompletionStage<Void> clearState(StateKey key);

    /**
     * Returns current payload data. Implementations should return {@link StateData#empty()} when absent.
     */
    CompletionStage<StateData> getStateData(StateKey key);

    /**
     * Replaces payload data for key.
     */
    CompletionStage<Void> setStateData(StateKey key, StateData data);

    /**
     * Clears payload data while preserving state id.
     */
    CompletionStage<Void> clearStateData(StateKey key);

    default CompletionStage<StateData> updateStateData(StateKey key, Map<String, Object> patch) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(patch, "patch");

        return getStateData(key)
                .thenCompose(existing -> {
                    StateData merged = existing.merge(patch);
                    return setStateData(key, merged).thenApply(ignored -> merged);
                });
    }

    default CompletionStage<StateSnapshot> getSnapshot(StateKey key) {
        Objects.requireNonNull(key, "key");

        CompletionStage<Optional<String>> stateStage = getState(key);
        CompletionStage<StateData> dataStage = getStateData(key);

        return stateStage.thenCombine(dataStage, (state, data) -> new StateSnapshot(key, state, data));
    }

    default CompletionStage<Void> clear(StateKey key) {
        Objects.requireNonNull(key, "key");
        return clearState(key).thenCompose(ignored -> clearStateData(key));
    }

}
