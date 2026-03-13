package ru.max.botframework.fsm;

import java.util.Objects;
import java.util.Optional;

/**
 * Current state and payload snapshot for a scope key.
 */
public record StateSnapshot(StateKey key, Optional<String> state, StateData data) {

    public StateSnapshot {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(data, "data");
        state = state.map(StateSnapshot::validateState);
    }

    public static StateSnapshot empty(StateKey key) {
        return new StateSnapshot(key, Optional.empty(), StateData.empty());
    }

    public StateSnapshot withState(String state) {
        return new StateSnapshot(key, Optional.of(validateState(state)), data);
    }

    public StateSnapshot withoutState() {
        return new StateSnapshot(key, Optional.empty(), data);
    }

    public StateSnapshot withData(StateData data) {
        return new StateSnapshot(key, state, Objects.requireNonNull(data, "data"));
    }

    private static String validateState(String state) {
        Objects.requireNonNull(state, "state");
        if (state.isBlank()) {
            throw new IllegalArgumentException("state must not be blank");
        }
        return state;
    }
}
