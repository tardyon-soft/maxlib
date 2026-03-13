package ru.max.botframework.fsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Test;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.UserId;

class FSMStorageContractTest {

    @Test
    void returnsEmptyStateAndDataByDefault() {
        FSMStorage storage = new InMemoryFSMStorage();
        StateKey key = StateKey.user(new UserId("user-1"));

        Optional<String> state = storage.getState(key).toCompletableFuture().join();
        StateData data = storage.getStateData(key).toCompletableFuture().join();

        assertTrue(state.isEmpty());
        assertTrue(data.values().isEmpty());
    }

    @Test
    void storesAndClearsStateWithoutAffectingData() {
        FSMStorage storage = new InMemoryFSMStorage();
        StateKey key = StateKey.userInChat(new UserId("user-1"), new ChatId("chat-1"));

        storage.setState(key, "checkout.email").toCompletableFuture().join();
        storage.setStateData(key, StateData.of(Map.of("email", "u@example.com"))).toCompletableFuture().join();
        storage.clearState(key).toCompletableFuture().join();

        Optional<String> state = storage.getState(key).toCompletableFuture().join();
        StateData data = storage.getStateData(key).toCompletableFuture().join();

        assertTrue(state.isEmpty());
        assertEquals("u@example.com", data.get("email", String.class).orElseThrow());
    }

    @Test
    void replacesAndMergesStateData() {
        FSMStorage storage = new InMemoryFSMStorage();
        StateKey key = StateKey.chat(new ChatId("chat-1"));

        storage.setStateData(key, StateData.of(Map.of("step", 1))).toCompletableFuture().join();
        StateData merged = storage.updateStateData(key, Map.of("step", 2, "confirmed", true))
                .toCompletableFuture()
                .join();

        assertEquals(2, merged.get("step", Integer.class).orElseThrow());
        assertEquals(true, merged.get("confirmed", Boolean.class).orElseThrow());

        StateData current = storage.getStateData(key).toCompletableFuture().join();
        assertEquals(2, current.get("step", Integer.class).orElseThrow());
    }

    @Test
    void separatesStateByDifferentKeys() {
        FSMStorage storage = new InMemoryFSMStorage();
        StateKey keyA = StateKey.userInChat(new UserId("user-a"), new ChatId("chat-1"));
        StateKey keyB = StateKey.userInChat(new UserId("user-b"), new ChatId("chat-1"));

        storage.setState(keyA, "s-a").toCompletableFuture().join();
        storage.setState(keyB, "s-b").toCompletableFuture().join();

        assertEquals("s-a", storage.getState(keyA).toCompletableFuture().join().orElseThrow());
        assertEquals("s-b", storage.getState(keyB).toCompletableFuture().join().orElseThrow());
    }

    @Test
    void clearsStateAndDataTogetherWithClearShortcut() {
        FSMStorage storage = new InMemoryFSMStorage();
        StateKey key = StateKey.user(new UserId("user-1"));

        storage.setState(key, "flow.step").toCompletableFuture().join();
        storage.setStateData(key, StateData.of(Map.of("x", 1))).toCompletableFuture().join();

        storage.clear(key).toCompletableFuture().join();

        assertTrue(storage.getState(key).toCompletableFuture().join().isEmpty());
        assertTrue(storage.getStateData(key).toCompletableFuture().join().values().isEmpty());
    }

    @Test
    void buildsSnapshotFromSeparatedStateAndData() {
        FSMStorage storage = new InMemoryFSMStorage();
        StateKey key = StateKey.user(new UserId("user-1"));
        storage.setState(key, "checkout.confirm").toCompletableFuture().join();
        storage.setStateData(key, StateData.of(Map.of("orderId", "42"))).toCompletableFuture().join();

        StateSnapshot snapshot = storage.getSnapshot(key).toCompletableFuture().join();

        assertEquals("checkout.confirm", snapshot.state().orElseThrow());
        assertEquals("42", snapshot.data().get("orderId", String.class).orElseThrow());
        assertFalse(snapshot.data().values().isEmpty());
    }

    private static final class InMemoryFSMStorage implements FSMStorage {
        private final ConcurrentMap<StateKey, String> states = new ConcurrentHashMap<>();
        private final ConcurrentMap<StateKey, StateData> data = new ConcurrentHashMap<>();

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
