package ru.max.botframework.fsm;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Async storage contract for FSM state and state payload.
 */
public interface FSMStorage {

    CompletionStage<Optional<String>> getState(StateKey key);

    CompletionStage<Void> setState(StateKey key, String state);

    CompletionStage<Void> clearState(StateKey key);

    CompletionStage<StateData> getStateData(StateKey key);

    CompletionStage<Void> setStateData(StateKey key, StateData data);

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
