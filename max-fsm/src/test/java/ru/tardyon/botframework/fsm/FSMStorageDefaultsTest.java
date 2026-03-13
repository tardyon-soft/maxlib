package ru.tardyon.botframework.fsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.UserId;

class FSMStorageDefaultsTest {

    @Test
    void updateStateDataUsesDefaultMergeContract() {
        InMemoryStorageStub storage = new InMemoryStorageStub();
        StateKey key = StateKey.userInChat(new UserId("u-default"), new ChatId("c-default"));
        storage.setStateData(key, StateData.of(Map.of("step", 1))).toCompletableFuture().join();

        StateData merged = storage.updateStateData(key, Map.of("step", 2, "confirmed", true))
                .toCompletableFuture()
                .join();

        assertEquals(2, merged.get("step", Integer.class).orElseThrow());
        assertEquals(true, merged.get("confirmed", Boolean.class).orElseThrow());
    }

    @Test
    void getSnapshotCombinesStateAndData() {
        InMemoryStorageStub storage = new InMemoryStorageStub();
        StateKey key = StateKey.user(new UserId("u-snapshot"));
        storage.setState(key, "checkout.confirm").toCompletableFuture().join();
        storage.setStateData(key, StateData.of(Map.of("orderId", "42"))).toCompletableFuture().join();

        StateSnapshot snapshot = storage.getSnapshot(key).toCompletableFuture().join();

        assertEquals("checkout.confirm", snapshot.state().orElseThrow());
        assertEquals("42", snapshot.data().get("orderId", String.class).orElseThrow());
    }

    @Test
    void clearRemovesStateAndData() {
        InMemoryStorageStub storage = new InMemoryStorageStub();
        StateKey key = StateKey.chat(new ChatId("c-clear"));
        storage.setState(key, "flow.step").toCompletableFuture().join();
        storage.setStateData(key, StateData.of(Map.of("x", 1))).toCompletableFuture().join();

        storage.clear(key).toCompletableFuture().join();

        assertTrue(storage.getState(key).toCompletableFuture().join().isEmpty());
        assertTrue(storage.getStateData(key).toCompletableFuture().join().values().isEmpty());
    }

    private static final class InMemoryStorageStub implements FSMStorage {
        private final java.util.concurrent.ConcurrentHashMap<StateKey, String> states = new java.util.concurrent.ConcurrentHashMap<>();
        private final java.util.concurrent.ConcurrentHashMap<StateKey, StateData> data = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public CompletionStage<Optional<String>> getState(StateKey key) {
            return CompletableFuture.completedFuture(Optional.ofNullable(states.get(key)));
        }

        @Override
        public CompletionStage<Void> setState(StateKey key, String state) {
            states.put(key, state);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> clearState(StateKey key) {
            states.remove(key);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<StateData> getStateData(StateKey key) {
            return CompletableFuture.completedFuture(data.getOrDefault(key, StateData.empty()));
        }

        @Override
        public CompletionStage<Void> setStateData(StateKey key, StateData stateData) {
            data.put(key, stateData);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> clearStateData(StateKey key) {
            data.remove(key);
            return CompletableFuture.completedFuture(null);
        }
    }
}
