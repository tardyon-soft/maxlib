package ru.tardyon.botframework.fsm;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory {@link FSMStorage} implementation.
 */
public final class MemoryStorage implements FSMStorage {

    private final ConcurrentMap<StateKey, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public CompletionStage<Optional<String>> getState(StateKey key) {
        Objects.requireNonNull(key, "key");
        return CompletableFuture.completedFuture(entries.getOrDefault(key, Entry.empty()).state());
    }

    @Override
    public CompletionStage<Void> setState(StateKey key, String state) {
        Objects.requireNonNull(key, "key");
        String normalized = normalizeState(state);

        entries.compute(key, (ignored, current) -> {
            Entry base = current == null ? Entry.empty() : current;
            return base.withState(normalized);
        });

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> clearState(StateKey key) {
        Objects.requireNonNull(key, "key");

        entries.computeIfPresent(key, (ignored, current) -> {
            Entry updated = current.withoutState();
            return updated.isEmpty() ? null : updated;
        });

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<StateData> getStateData(StateKey key) {
        Objects.requireNonNull(key, "key");
        return CompletableFuture.completedFuture(entries.getOrDefault(key, Entry.empty()).data());
    }

    @Override
    public CompletionStage<Void> setStateData(StateKey key, StateData data) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(data, "data");

        entries.compute(key, (ignored, current) -> {
            Entry base = current == null ? Entry.empty() : current;
            Entry updated = base.withData(data);
            return updated.isEmpty() ? null : updated;
        });

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<StateData> updateStateData(StateKey key, Map<String, Object> patch) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(patch, "patch");

        if (patch.isEmpty()) {
            return getStateData(key);
        }

        Entry updated = entries.compute(key, (ignored, current) -> {
            Entry base = current == null ? Entry.empty() : current;
            return base.withData(base.data().merge(patch));
        });

        return CompletableFuture.completedFuture(updated == null ? StateData.empty() : updated.data());
    }

    @Override
    public CompletionStage<Void> clearStateData(StateKey key) {
        Objects.requireNonNull(key, "key");

        entries.computeIfPresent(key, (ignored, current) -> {
            Entry updated = current.withData(StateData.empty());
            return updated.isEmpty() ? null : updated;
        });

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<StateSnapshot> getSnapshot(StateKey key) {
        Objects.requireNonNull(key, "key");
        Entry entry = entries.getOrDefault(key, Entry.empty());
        return CompletableFuture.completedFuture(new StateSnapshot(key, entry.state(), entry.data()));
    }

    @Override
    public CompletionStage<Void> clear(StateKey key) {
        Objects.requireNonNull(key, "key");
        entries.remove(key);
        return CompletableFuture.completedFuture(null);
    }

    private static String normalizeState(String state) {
        Objects.requireNonNull(state, "state");
        if (state.isBlank()) {
            throw new IllegalArgumentException("state must not be blank");
        }
        return state;
    }

    private record Entry(Optional<String> state, StateData data) {
        private Entry {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(data, "data");
        }

        static Entry empty() {
            return new Entry(Optional.empty(), StateData.empty());
        }

        Entry withState(String state) {
            return new Entry(Optional.of(normalizeState(state)), data);
        }

        Entry withoutState() {
            return new Entry(Optional.empty(), data);
        }

        Entry withData(StateData data) {
            return new Entry(state, data);
        }

        boolean isEmpty() {
            return state.isEmpty() && data.values().isEmpty();
        }
    }
}
