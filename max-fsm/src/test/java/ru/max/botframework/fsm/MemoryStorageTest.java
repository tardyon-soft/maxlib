package ru.max.botframework.fsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.UserId;

class MemoryStorageTest {

    @Test
    void setGetAndClearState() {
        MemoryStorage storage = new MemoryStorage();
        StateKey key = StateKey.user(new UserId("u-1"));

        storage.setState(key, "flow.step1").toCompletableFuture().join();
        assertEquals("flow.step1", storage.getState(key).toCompletableFuture().join().orElseThrow());

        storage.clearState(key).toCompletableFuture().join();
        assertTrue(storage.getState(key).toCompletableFuture().join().isEmpty());
    }

    @Test
    void setGetAndUpdateData() {
        MemoryStorage storage = new MemoryStorage();
        StateKey key = StateKey.chat(new ChatId("chat-1"));

        storage.setStateData(key, StateData.of(Map.of("step", 1))).toCompletableFuture().join();
        StateData updated = storage.updateStateData(key, Map.of("step", 2, "ok", true))
                .toCompletableFuture()
                .join();

        assertEquals(2, updated.get("step", Integer.class).orElseThrow());
        assertEquals(true, updated.get("ok", Boolean.class).orElseThrow());
        assertEquals(2, storage.getStateData(key).toCompletableFuture().join().get("step", Integer.class).orElseThrow());
    }

    @Test
    void keepsDifferentStateKeysIndependent() {
        MemoryStorage storage = new MemoryStorage();
        StateKey a = StateKey.userInChat(new UserId("u-a"), new ChatId("chat-1"));
        StateKey b = StateKey.userInChat(new UserId("u-b"), new ChatId("chat-1"));

        storage.setState(a, "state-a").toCompletableFuture().join();
        storage.setState(b, "state-b").toCompletableFuture().join();
        storage.setStateData(a, StateData.of(Map.of("x", "a"))).toCompletableFuture().join();
        storage.setStateData(b, StateData.of(Map.of("x", "b"))).toCompletableFuture().join();

        assertEquals("state-a", storage.getState(a).toCompletableFuture().join().orElseThrow());
        assertEquals("state-b", storage.getState(b).toCompletableFuture().join().orElseThrow());
        assertEquals("a", storage.getStateData(a).toCompletableFuture().join().get("x", String.class).orElseThrow());
        assertEquals("b", storage.getStateData(b).toCompletableFuture().join().get("x", String.class).orElseThrow());
    }

    @Test
    void supportsReasonableConcurrentUpdates() throws Exception {
        MemoryStorage storage = new MemoryStorage();
        StateKey key = StateKey.userInChat(new UserId("u-1"), new ChatId("chat-1"));

        int workers = 8;
        int updates = 80;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < updates; i++) {
                int index = i;
                futures.add(CompletableFuture.runAsync(
                        () -> storage.updateStateData(key, Map.of("k" + index, index)).toCompletableFuture().join(),
                        executor
                ));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            StateData data = storage.getStateData(key).toCompletableFuture().join();
            assertEquals(updates, data.values().size());
            assertEquals(42, data.get("k42", Integer.class).orElseThrow());
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
