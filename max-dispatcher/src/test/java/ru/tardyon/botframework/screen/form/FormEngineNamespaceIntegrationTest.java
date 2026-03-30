package ru.tardyon.botframework.screen.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.fsm.FSMContext;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.StateKey;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.UserId;

class FormEngineNamespaceIntegrationTest {

    @Test
    void formStateInScreenNamespaceDoesNotAffectDefaultFsmData() {
        MemoryStorage storage = new MemoryStorage();
        FSMContext defaultFsm = FSMContext.of(storage, StateKey.userInChat(new UserId("u-4"), new ChatId("c-4")));
        FSMContext screenFsm = FSMContext.of(storage, StateKey.userInChat(new UserId("screen::u-4"), new ChatId("screen::c-4")));

        FormEngine engine = new FormEngine(new FsmFormStateStorage());
        FormDefinition definition = FormDefinition.of(
                "channel-form",
                FormStep.of("channel", "Введите канал"),
                FormStep.of("timezone", "Введите таймзону")
        );

        defaultFsm.setState("demo.form.legacy").toCompletableFuture().join();
        defaultFsm.updateData(java.util.Map.of("legacyName", "Alice")).toCompletableFuture().join();

        engine.start(screenFsm, definition).toCompletableFuture().join();
        engine.next(screenFsm, definition, "@demo_channel").toCompletableFuture().join();

        assertEquals("demo.form.legacy", defaultFsm.currentState().toCompletableFuture().join().orElseThrow());
        assertEquals("Alice", defaultFsm.data().toCompletableFuture().join().get("legacyName", String.class).orElseThrow());
        assertTrue(defaultFsm.data().toCompletableFuture().join().get(FsmFormStateStorage.FORM_STATE_KEY).isEmpty());
        assertTrue(screenFsm.data().toCompletableFuture().join().get(FsmFormStateStorage.FORM_STATE_KEY).isPresent());
    }
}
