package ru.tardyon.botframework.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.fsm.FSMContext;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.StateKey;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.UserId;
import ru.tardyon.botframework.screen.FsmScreenStorage;
import ru.tardyon.botframework.screen.LegacyStringScreenActionCodec;

class ScreenFixturesTest {

    @Test
    void buildsCallbackActionPayloadWithCodec() {
        String payload = ScreenFixtures.actionPayload(
                "open_profile",
                Map.of("id", "42"),
                new LegacyStringScreenActionCodec()
        );

        assertEquals("ui:act:open_profile?id=42", payload);
    }

    @Test
    void seedsScreenStateIntoFsmStorage() {
        MemoryStorage storage = new MemoryStorage();
        FSMContext screenFsm = FSMContext.of(
                storage,
                StateKey.userInChat(new UserId("screen::u-1"), new ChatId("screen::c-1"))
        );
        var session = ScreenFixtures.activeSession("profile", Map.of("name", "Alice"));

        ScreenFixtures.seedState(screenFsm, session).toCompletableFuture().join();

        var loaded = new FsmScreenStorage().get(screenFsm).toCompletableFuture().join().orElseThrow();
        assertEquals("profile", loaded.top().orElseThrow().screenId());
        assertEquals("Alice", loaded.top().orElseThrow().params().get("name"));
        assertTrue(loaded.rootMessageId() != null && !loaded.rootMessageId().isBlank());
    }
}
