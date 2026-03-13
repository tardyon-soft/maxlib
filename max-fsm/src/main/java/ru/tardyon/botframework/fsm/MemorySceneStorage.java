package ru.tardyon.botframework.fsm;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory scene metadata storage.
 */
public final class MemorySceneStorage implements SceneStorage {
    private final ConcurrentMap<StateKey, SceneSession> sessions = new ConcurrentHashMap<>();

    @Override
    public CompletionStage<Optional<SceneSession>> get(StateKey key) {
        Objects.requireNonNull(key, "key");
        return CompletableFuture.completedFuture(Optional.ofNullable(sessions.get(key)));
    }

    @Override
    public CompletionStage<Void> set(StateKey key, SceneSession session) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(session, "session");
        sessions.put(key, session);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> clear(StateKey key) {
        Objects.requireNonNull(key, "key");
        sessions.remove(key);
        return CompletableFuture.completedFuture(null);
    }
}
