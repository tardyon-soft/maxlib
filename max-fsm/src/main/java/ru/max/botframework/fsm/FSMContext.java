package ru.max.botframework.fsm;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Runtime-facing API for reading and mutating FSM state within a resolved scope.
 */
public interface FSMContext {

    StateKey scope();

    CompletionStage<Optional<String>> currentState();

    CompletionStage<Void> setState(String state);

    CompletionStage<Void> clearState();

    CompletionStage<StateData> data();

    CompletionStage<Void> setData(StateData data);

    default CompletionStage<Void> setData(Map<String, Object> data) {
        Objects.requireNonNull(data, "data");
        return setData(StateData.of(data));
    }

    CompletionStage<StateData> updateData(Map<String, Object> patch);

    CompletionStage<Void> clearData();

    default CompletionStage<StateSnapshot> snapshot() {
        return currentState().thenCombine(data(), (state, payload) -> new StateSnapshot(scope(), state, payload));
    }

    default CompletionStage<Void> clear() {
        return clearState().thenCompose(ignored -> clearData());
    }

    static FSMContext of(FSMStorage storage, StateKey scope) {
        return new StorageBackedFSMContext(storage, scope);
    }
}
