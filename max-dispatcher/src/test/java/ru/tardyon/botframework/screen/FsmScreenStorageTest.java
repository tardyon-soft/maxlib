package ru.tardyon.botframework.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.fsm.FSMContext;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.StateKey;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.UserId;

class FsmScreenStorageTest {

    @Test
    void screenSessionIsStoredInDedicatedScreenNamespaceScope() {
        MemoryStorage storage = new MemoryStorage();
        FSMContext userFsm = FSMContext.of(storage, StateKey.userInChat(new UserId("u-1"), new ChatId("c-1")));
        FSMContext screenFsm = FSMContext.of(storage, StateKey.userInChat(new UserId("screen::u-1"), new ChatId("screen::c-1")));
        FsmScreenStorage screenStorage = new FsmScreenStorage(userFsm);

        userFsm.setState("form.name").toCompletableFuture().join();
        userFsm.updateData(Map.of("name", "Alice")).toCompletableFuture().join();

        ScreenSession session = new ScreenSession(
                "scope-1",
                List.of(new ScreenStackEntry("home", Map.of("x", 1), Instant.parse("2026-03-26T10:00:00Z"))),
                "msg-1",
                Instant.parse("2026-03-26T10:00:01Z")
        );

        screenStorage.set(screenFsm, session).toCompletableFuture().join();

        assertEquals("form.name", userFsm.currentState().toCompletableFuture().join().orElseThrow());
        assertEquals("Alice", userFsm.data().toCompletableFuture().join().get("name", String.class).orElseThrow());
        assertTrue(userFsm.data().toCompletableFuture().join().get(FsmScreenStorage.SCREEN_SESSION_KEY).isEmpty());
        assertTrue(screenFsm.data().toCompletableFuture().join().get(FsmScreenStorage.SCREEN_SESSION_KEY).isPresent());
    }

    @Test
    void getMigratesLegacyScreenSessionToScreenNamespace() {
        MemoryStorage storage = new MemoryStorage();
        FSMContext legacyFsm = FSMContext.of(storage, StateKey.userInChat(new UserId("u-2"), new ChatId("c-2")));
        FSMContext screenFsm = FSMContext.of(storage, StateKey.userInChat(new UserId("screen::u-2"), new ChatId("screen::c-2")));
        FsmScreenStorage screenStorage = new FsmScreenStorage(legacyFsm);

        ScreenSession legacySession = new ScreenSession(
                "scope-2",
                List.of(new ScreenStackEntry("home", Map.of(), Instant.parse("2026-03-26T11:00:00Z"))),
                "msg-2",
                Instant.parse("2026-03-26T11:00:01Z")
        );
        screenStorage.set(legacyFsm, legacySession).toCompletableFuture().join();

        ScreenSession loaded = screenStorage.get(screenFsm).toCompletableFuture().join().orElseThrow();

        assertEquals("msg-2", loaded.rootMessageId());
        assertTrue(legacyFsm.data().toCompletableFuture().join().get(FsmScreenStorage.SCREEN_SESSION_KEY).isPresent());
        assertTrue(screenFsm.data().toCompletableFuture().join().get(FsmScreenStorage.SCREEN_SESSION_KEY).isPresent());
    }
}
