package ru.max.botframework.fsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.UserId;

class FSMContextTest {

    @Test
    void supportsBasicStateOperations() {
        FSMStorage storage = new MemoryStorage();
        StateKey key = StateKey.user(new UserId("user-1"));
        FSMContext context = FSMContext.of(storage, key);

        context.setState("checkout.email").toCompletableFuture().join();

        assertEquals("checkout.email", context.currentState().toCompletableFuture().join().orElseThrow());

        context.clearState().toCompletableFuture().join();
        assertTrue(context.currentState().toCompletableFuture().join().isEmpty());
    }

    @Test
    void supportsDataReplaceAndMergeOperations() {
        FSMStorage storage = new MemoryStorage();
        FSMContext context = FSMContext.of(storage, StateKey.chat(new ChatId("chat-1")));

        context.setData(Map.of("step", 1)).toCompletableFuture().join();
        StateData merged = context.updateData(Map.of("step", 2, "confirmed", true)).toCompletableFuture().join();

        assertEquals(2, merged.get("step", Integer.class).orElseThrow());
        assertEquals(true, merged.get("confirmed", Boolean.class).orElseThrow());

        StateData stored = context.data().toCompletableFuture().join();
        assertEquals(2, stored.get("step", Integer.class).orElseThrow());

        context.clearData().toCompletableFuture().join();
        assertTrue(context.data().toCompletableFuture().join().values().isEmpty());
    }

    @Test
    void snapshotReflectsStateAndData() {
        FSMStorage storage = new MemoryStorage();
        StateKey key = StateKey.userInChat(new UserId("user-1"), new ChatId("chat-1"));
        FSMContext context = new StorageBackedFSMContext(storage, key);

        context.setState("checkout.confirm").toCompletableFuture().join();
        context.setData(Map.of("orderId", "42")).toCompletableFuture().join();

        StateSnapshot snapshot = context.snapshot().toCompletableFuture().join();

        assertEquals("checkout.confirm", snapshot.state().orElseThrow());
        assertEquals("42", snapshot.data().get("orderId", String.class).orElseThrow());
        assertEquals(key, snapshot.key());
    }

    @Test
    void delegatesOperationsUsingResolvedScopeKey() {
        RecordingStorage storage = new RecordingStorage(new MemoryStorage());
        StateKey key = StateKey.userInChat(new UserId("user-5"), new ChatId("chat-5"));
        FSMContext context = new StorageBackedFSMContext(storage, key);

        context.currentState().toCompletableFuture().join();
        context.setState("state-1").toCompletableFuture().join();
        context.data().toCompletableFuture().join();
        context.setData(Map.of("k", "v")).toCompletableFuture().join();
        context.updateData(Map.of("n", 1)).toCompletableFuture().join();
        context.clearData().toCompletableFuture().join();
        context.clearState().toCompletableFuture().join();

        assertTrue(storage.seenKeys.stream().allMatch(key::equals));
        assertTrue(storage.seenKeys.size() >= 7);
    }

    @Test
    void exposesConfiguredScope() {
        StateKey key = StateKey.user(new UserId("u-1"));
        FSMContext context = FSMContext.of(new MemoryStorage(), key);

        assertSame(key, context.scope());
    }

    private static final class RecordingStorage implements FSMStorage {
        private final FSMStorage delegate;
        private final List<StateKey> seenKeys = new ArrayList<>();

        private RecordingStorage(FSMStorage delegate) {
            this.delegate = delegate;
        }

        @Override
        public CompletionStage<Optional<String>> getState(StateKey key) {
            seenKeys.add(key);
            return delegate.getState(key);
        }

        @Override
        public CompletionStage<Void> setState(StateKey key, String state) {
            seenKeys.add(key);
            return delegate.setState(key, state);
        }

        @Override
        public CompletionStage<Void> clearState(StateKey key) {
            seenKeys.add(key);
            return delegate.clearState(key);
        }

        @Override
        public CompletionStage<StateData> getStateData(StateKey key) {
            seenKeys.add(key);
            return delegate.getStateData(key);
        }

        @Override
        public CompletionStage<Void> setStateData(StateKey key, StateData data) {
            seenKeys.add(key);
            return delegate.setStateData(key, data);
        }

        @Override
        public CompletionStage<StateData> updateStateData(StateKey key, Map<String, Object> patch) {
            seenKeys.add(key);
            return delegate.updateStateData(key, patch);
        }

        @Override
        public CompletionStage<Void> clearStateData(StateKey key) {
            seenKeys.add(key);
            return delegate.clearStateData(key);
        }
    }
}
