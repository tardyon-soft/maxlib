package ru.tardyon.botframework.screen.form;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.fsm.FSMContext;

/**
 * Persistence port for form state.
 */
public interface FormStateStorage {

    CompletionStage<Optional<FormState>> get(FSMContext fsm);

    CompletionStage<Void> set(FSMContext fsm, FormState state);

    CompletionStage<Void> clear(FSMContext fsm);
}
