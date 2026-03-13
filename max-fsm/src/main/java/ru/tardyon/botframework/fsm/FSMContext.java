package ru.tardyon.botframework.fsm;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Runtime-facing API for reading and mutating FSM state within a resolved scope.
 *
 * <p>Implementations are scope-bound (see {@link #scope()}) and delegate persistence
 * to configured {@link FSMStorage}. Missing state is represented as {@link Optional#empty()}.</p>
 */
public interface FSMContext {

    /**
     * Scope key used for all state/data operations.
     */
    StateKey scope();

    /**
     * Reads current state id.
     */
    CompletionStage<Optional<String>> currentState();

    /**
     * Replaces current state id for this scope.
     */
    CompletionStage<Void> setState(String state);

    /**
     * Clears current state id while preserving payload data.
     */
    CompletionStage<Void> clearState();

    /**
     * Reads current payload data.
     */
    CompletionStage<StateData> data();

    /**
     * Replaces payload data.
     */
    CompletionStage<Void> setData(StateData data);

    default CompletionStage<Void> setData(Map<String, Object> data) {
        Objects.requireNonNull(data, "data");
        return setData(StateData.of(data));
    }

    /**
     * Merges patch into existing payload data.
     */
    CompletionStage<StateData> updateData(Map<String, Object> patch);

    /**
     * Clears payload data while preserving state id.
     */
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
