package ru.tardyon.botframework.micronaut.storage;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import ru.tardyon.botframework.client.serialization.JsonCodec;
import ru.tardyon.botframework.fsm.FSMStorage;
import ru.tardyon.botframework.fsm.FsmStorageException;
import ru.tardyon.botframework.fsm.StateData;
import ru.tardyon.botframework.fsm.StateKey;

/**
 * Redis-backed {@link FSMStorage} implementation for Micronaut starter.
 */
public final class RedisFSMStorage implements FSMStorage {
    private final StatefulRedisConnection<String, String> redis;
    private final JsonCodec jsonCodec;
    private final String keyPrefix;
    private final Duration ttl;

    public RedisFSMStorage(
            StatefulRedisConnection<String, String> redis,
            JsonCodec jsonCodec,
            String keyPrefix,
            Duration ttl
    ) {
        this.redis = java.util.Objects.requireNonNull(redis, "redis");
        this.jsonCodec = java.util.Objects.requireNonNull(jsonCodec, "jsonCodec");
        this.keyPrefix = normalizePrefix(keyPrefix);
        this.ttl = ttl;
    }

    @Override
    public CompletionStage<Optional<String>> getState(StateKey key) {
        try {
            String value = commands().get(stateKey(key));
            return CompletableFuture.completedFuture(Optional.ofNullable(value));
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(new FsmStorageException("redis.getState", e));
        }
    }

    @Override
    public CompletionStage<Void> setState(StateKey key, String state) {
        try {
            writeValue(stateKey(key), normalizeState(state));
            return CompletableFuture.completedFuture(null);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(new FsmStorageException("redis.setState", e));
        }
    }

    @Override
    public CompletionStage<Void> clearState(StateKey key) {
        try {
            commands().del(stateKey(key));
            return CompletableFuture.completedFuture(null);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(new FsmStorageException("redis.clearState", e));
        }
    }

    @Override
    public CompletionStage<StateData> getStateData(StateKey key) {
        try {
            String raw = commands().get(dataKey(key));
            if (raw == null || raw.isBlank()) {
                return CompletableFuture.completedFuture(StateData.empty());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) jsonCodec.read(raw, Map.class);
            return CompletableFuture.completedFuture(StateData.of(map == null ? Map.of() : map));
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(new FsmStorageException("redis.getStateData", e));
        }
    }

    @Override
    public CompletionStage<Void> setStateData(StateKey key, StateData data) {
        try {
            if (data.values().isEmpty()) {
                commands().del(dataKey(key));
            } else {
                writeValue(dataKey(key), jsonCodec.write(new LinkedHashMap<>(data.values())));
            }
            return CompletableFuture.completedFuture(null);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(new FsmStorageException("redis.setStateData", e));
        }
    }

    @Override
    public CompletionStage<Void> clearStateData(StateKey key) {
        try {
            commands().del(dataKey(key));
            return CompletableFuture.completedFuture(null);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(new FsmStorageException("redis.clearStateData", e));
        }
    }

    @Override
    public CompletionStage<Void> clear(StateKey key) {
        try {
            commands().del(stateKey(key), dataKey(key));
            return CompletableFuture.completedFuture(null);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(new FsmStorageException("redis.clear", e));
        }
    }

    private void writeValue(String key, String value) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            commands().set(key, value);
            return;
        }
        commands().psetex(key, ttl.toMillis(), value);
    }

    private RedisCommands<String, String> commands() {
        return redis.sync();
    }

    private String stateKey(StateKey key) {
        return keyBase(key) + ":state";
    }

    private String dataKey(StateKey key) {
        return keyBase(key) + ":data";
    }

    private String keyBase(StateKey key) {
        String scope = key.scope().name().toLowerCase(java.util.Locale.ROOT);
        String user = key.userId() == null ? "-" : key.userId().value();
        String chat = key.chatId() == null ? "-" : key.chatId().value();
        return keyPrefix + ":" + scope + ":u:" + user + ":c:" + chat;
    }

    private static String normalizePrefix(String value) {
        java.util.Objects.requireNonNull(value, "keyPrefix");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("keyPrefix must not be blank");
        }
        return normalized;
    }

    private static String normalizeState(String state) {
        java.util.Objects.requireNonNull(state, "state");
        if (state.isBlank()) {
            throw new IllegalArgumentException("state must not be blank");
        }
        return state;
    }
}
