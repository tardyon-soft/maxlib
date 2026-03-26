package ru.tardyon.botframework.screen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CompletableFuture;
import ru.tardyon.botframework.fsm.FSMContext;
import ru.tardyon.botframework.fsm.StateData;

/**
 * Screen storage backed by FSM payload data.
 */
public final class FsmScreenStorage implements ScreenStorage {
    static final String SCREEN_SESSION_KEY = "ui.screen.session.v1";
    private static final ConcurrentMap<String, ScreenSession> CHAT_CACHE = new ConcurrentHashMap<>();
    private final FSMContext legacyFsm;

    public FsmScreenStorage() {
        this.legacyFsm = null;
    }

    public FsmScreenStorage(FSMContext legacyFsm) {
        this.legacyFsm = legacyFsm;
    }

    @Override
    public CompletionStage<Optional<ScreenSession>> get(FSMContext fsm) {
        return fsm.data().thenCompose(data -> {
            Optional<ScreenSession> fromFsm = ScreenSessionCodec.decode(data.get(SCREEN_SESSION_KEY).orElse(null));
            if (fromFsm.isPresent()) {
                fromFsm.ifPresent(session -> cache(fsm, session));
                return CompletableFuture.completedFuture(fromFsm);
            }
            Optional<ScreenSession> fromCache = Optional.ofNullable(CHAT_CACHE.get(cacheKey(fsm)));
            if (fromCache.isPresent()) {
                return CompletableFuture.completedFuture(fromCache);
            }
            if (legacyFsm == null) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            return legacyFsm.data().thenCompose(legacyData -> {
                Optional<ScreenSession> fromLegacy = ScreenSessionCodec.decode(legacyData.get(SCREEN_SESSION_KEY).orElse(null));
                if (fromLegacy.isEmpty()) {
                    return CompletableFuture.completedFuture(Optional.empty());
                }
                ScreenSession migrated = fromLegacy.orElseThrow();
                return set(fsm, migrated)
                        .handle((ignored, throwable) -> Optional.of(migrated));
            });
        });
    }

    @Override
    public CompletionStage<Void> set(FSMContext fsm, ScreenSession session) {
        cache(fsm, session);
        return fsm.updateData(Map.of(SCREEN_SESSION_KEY, ScreenSessionCodec.encode(session))).thenAccept(ignored -> {
        });
    }

    @Override
    public CompletionStage<Void> clear(FSMContext fsm) {
        CHAT_CACHE.remove(cacheKey(fsm));
        CompletionStage<Void> clearCurrent = clearKey(fsm);
        if (legacyFsm == null) {
            return clearCurrent;
        }
        return clearCurrent.thenCompose(ignored -> clearKey(legacyFsm));
    }

    private static void cache(FSMContext fsm, ScreenSession session) {
        CHAT_CACHE.put(cacheKey(fsm), session);
    }

    private static CompletionStage<Void> clearKey(FSMContext fsm) {
        return fsm.data().thenCompose(existing -> {
            LinkedHashMap<String, Object> values = new LinkedHashMap<>(existing.values());
            values.remove(SCREEN_SESSION_KEY);
            return fsm.setData(StateData.of(values));
        });
    }

    private static String cacheKey(FSMContext fsm) {
        if (fsm.scope().chatId() != null && fsm.scope().chatId().value() != null) {
            return "chat:" + fsm.scope().chatId().value();
        }
        if (fsm.scope().userId() != null && fsm.scope().userId().value() != null) {
            return "user:" + fsm.scope().userId().value();
        }
        return "scope:" + fsm.scope();
    }
}
