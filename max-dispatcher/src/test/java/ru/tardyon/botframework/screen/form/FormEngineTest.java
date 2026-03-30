package ru.tardyon.botframework.screen.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.fsm.FSMContext;
import ru.tardyon.botframework.fsm.MemoryStorage;
import ru.tardyon.botframework.fsm.StateKey;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.UserId;

class FormEngineTest {

    @Test
    void multiStepFormSupportsNextBackAndSubmit() {
        FormDefinition definition = FormDefinition.of(
                "schedule-form",
                FormStep.of("channel", "Введите канал", FormValidator.required("Канал обязателен")),
                FormStep.of("time", "Введите время", FormValidator.required("Время обязательно"))
        );
        FormEngine engine = new FormEngine(new FsmFormStateStorage());
        FSMContext fsm = FSMContext.of(new MemoryStorage(), StateKey.userInChat(new UserId("u-1"), new ChatId("c-1")));

        FormState started = engine.start(fsm, definition).toCompletableFuture().join();
        assertEquals(0, started.stepIndex());
        assertTrue(started.values().isEmpty());

        FormEngine.TransitionResult firstNext = engine.next(fsm, definition, "@news").toCompletableFuture().join();
        assertTrue(firstNext.moved());
        assertEquals(1, firstNext.state().stepIndex());
        assertEquals("@news", firstNext.state().values().get("channel"));

        FormEngine.TransitionResult back = engine.back(fsm, definition).toCompletableFuture().join();
        assertTrue(back.moved());
        assertEquals(0, back.state().stepIndex());

        FormEngine.TransitionResult nextAgain = engine.next(fsm, definition, "@news").toCompletableFuture().join();
        assertTrue(nextAgain.moved());
        assertEquals(1, nextAgain.state().stepIndex());

        FormEngine.TransitionResult submitted = engine.submit(fsm, definition, "09:30").toCompletableFuture().join();
        assertTrue(submitted.finished());
        assertEquals(FormState.Status.SUBMITTED, submitted.state().status());
        assertEquals(Map.of("channel", "@news", "time", "09:30"), submitted.state().values());
        assertTrue(engine.current(fsm).toCompletableFuture().join().isEmpty());
    }

    @Test
    void validationErrorBlocksTransition() {
        FormDefinition definition = FormDefinition.of(
                "channel-form",
                FormStep.of("channel", "Введите канал", FormValidator.required("Введите ссылку на канал")),
                FormStep.of("timezone", "Введите таймзону")
        );
        FormEngine engine = new FormEngine(new FsmFormStateStorage());
        FSMContext fsm = FSMContext.of(new MemoryStorage(), StateKey.userInChat(new UserId("u-2"), new ChatId("c-2")));

        engine.start(fsm, definition).toCompletableFuture().join();

        FormEngine.TransitionResult blocked = engine.next(fsm, definition, "   ").toCompletableFuture().join();
        assertFalse(blocked.moved());
        assertTrue(blocked.blocked());
        assertEquals("Введите ссылку на канал", blocked.error());

        FormState persisted = engine.current(fsm).toCompletableFuture().join().orElseThrow();
        assertEquals(0, persisted.stepIndex());
        assertTrue(persisted.values().isEmpty());
    }

    @Test
    void formStateRestoresFromStorageOnNextUpdate() {
        MemoryStorage storage = new MemoryStorage();
        StateKey scope = StateKey.userInChat(new UserId("u-3"), new ChatId("c-3"));
        FormDefinition definition = FormDefinition.of(
                "channel-form",
                FormStep.of("channel", "Введите канал", FormValidator.required("Введите ссылку на канал")),
                FormStep.of("timezone", "Введите таймзону")
        );
        FormEngine engine = new FormEngine(new FsmFormStateStorage());

        FSMContext firstUpdateFsm = FSMContext.of(storage, scope);
        engine.start(firstUpdateFsm, definition).toCompletableFuture().join();
        engine.next(firstUpdateFsm, definition, "@dev_channel").toCompletableFuture().join();

        FSMContext secondUpdateFsm = FSMContext.of(storage, scope);
        FormState restored = engine.current(secondUpdateFsm).toCompletableFuture().join().orElseThrow();
        assertEquals("channel-form", restored.formId());
        assertEquals(1, restored.stepIndex());
        assertEquals("@dev_channel", restored.values().get("channel"));
    }
}
