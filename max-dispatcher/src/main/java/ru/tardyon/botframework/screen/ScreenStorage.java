package ru.tardyon.botframework.screen;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.fsm.FSMContext;

/**
 * Storage abstraction for screen sessions.
 */
public interface ScreenStorage {
    CompletionStage<Optional<ScreenSession>> get(FSMContext fsm);

    CompletionStage<Void> set(FSMContext fsm, ScreenSession session);

    CompletionStage<Void> clear(FSMContext fsm);
}
