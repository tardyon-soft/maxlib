package ru.tardyon.botframework.fsm;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Default {@link FSMContext} backed by {@link FSMStorage}.
 */
public final class StorageBackedFSMContext implements FSMContext {

    private final FSMStorage storage;
    private final StateKey scope;

    public StorageBackedFSMContext(FSMStorage storage, StateKey scope) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.scope = Objects.requireNonNull(scope, "scope");
    }

    @Override
    public StateKey scope() {
        return scope;
    }

    @Override
    public CompletionStage<Optional<String>> currentState() {
        return storage.getState(scope);
    }

    @Override
    public CompletionStage<Void> setState(String state) {
        return storage.setState(scope, state);
    }

    @Override
    public CompletionStage<Void> clearState() {
        return storage.clearState(scope);
    }

    @Override
    public CompletionStage<StateData> data() {
        return storage.getStateData(scope);
    }

    @Override
    public CompletionStage<Void> setData(StateData data) {
        return storage.setStateData(scope, data);
    }

    @Override
    public CompletionStage<StateData> updateData(Map<String, Object> patch) {
        return storage.updateStateData(scope, patch);
    }

    @Override
    public CompletionStage<Void> clearData() {
        return storage.clearStateData(scope);
    }
}
