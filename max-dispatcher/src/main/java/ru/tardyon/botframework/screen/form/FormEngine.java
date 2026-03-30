package ru.tardyon.botframework.screen.form;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.fsm.FSMContext;

/**
 * Form transition runtime.
 */
public final class FormEngine {
    private final FormStateStorage storage;

    public FormEngine(FormStateStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    public CompletionStage<FormState> start(FSMContext fsm, FormDefinition definition) {
        Objects.requireNonNull(fsm, "fsm");
        Objects.requireNonNull(definition, "definition");
        FormState initial = FormState.start(definition.id());
        return storage.set(fsm, initial).thenApply(ignored -> initial);
    }

    public CompletionStage<Optional<FormState>> current(FSMContext fsm) {
        Objects.requireNonNull(fsm, "fsm");
        return storage.get(fsm);
    }

    public CompletionStage<TransitionResult> next(FSMContext fsm, FormDefinition definition, String input) {
        return storage.get(Objects.requireNonNull(fsm, "fsm"))
                .thenCompose(stateOpt -> {
                    if (stateOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(TransitionResult.missingState());
                    }
                    FormState state = stateOpt.orElseThrow();
                    if (!state.inProgress()) {
                        return CompletableFuture.completedFuture(TransitionResult.blocked(state, "form is not in progress"));
                    }
                    if (!matchesForm(definition, state)) {
                        return CompletableFuture.completedFuture(TransitionResult.blocked(state, "form mismatch"));
                    }
                    if (definition.isLastStep(state.stepIndex())) {
                        return CompletableFuture.completedFuture(
                                TransitionResult.blocked(state, "last step reached, use submit transition")
                        );
                    }

                    FormStep step = definition.step(state.stepIndex());
                    FormValidator.ValidationResult validation = step.validator().validate(input, state);
                    if (!validation.valid()) {
                        return CompletableFuture.completedFuture(TransitionResult.blocked(state, validation.error()));
                    }

                    FormState updated = state.withValue(step.id(), input).withStepIndex(state.stepIndex() + 1);
                    return storage.set(fsm, updated).thenApply(ignored -> TransitionResult.moved(updated));
                });
    }

    public CompletionStage<TransitionResult> back(FSMContext fsm, FormDefinition definition) {
        return storage.get(Objects.requireNonNull(fsm, "fsm"))
                .thenCompose(stateOpt -> {
                    if (stateOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(TransitionResult.missingState());
                    }
                    FormState state = stateOpt.orElseThrow();
                    if (!state.inProgress()) {
                        return CompletableFuture.completedFuture(TransitionResult.blocked(state, "form is not in progress"));
                    }
                    if (!matchesForm(definition, state)) {
                        return CompletableFuture.completedFuture(TransitionResult.blocked(state, "form mismatch"));
                    }
                    if (state.stepIndex() == 0) {
                        return CompletableFuture.completedFuture(TransitionResult.blocked(state, "already at first step"));
                    }

                    FormState updated = state.withStepIndex(state.stepIndex() - 1);
                    return storage.set(fsm, updated).thenApply(ignored -> TransitionResult.moved(updated));
                });
    }

    public CompletionStage<TransitionResult> cancel(FSMContext fsm, FormDefinition definition) {
        return storage.get(Objects.requireNonNull(fsm, "fsm"))
                .thenCompose(stateOpt -> {
                    if (stateOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(TransitionResult.missingState());
                    }
                    FormState state = stateOpt.orElseThrow();
                    if (!matchesForm(definition, state)) {
                        return CompletableFuture.completedFuture(TransitionResult.blocked(state, "form mismatch"));
                    }
                    FormState cancelled = state.cancelled();
                    return storage.clear(fsm).thenApply(ignored -> TransitionResult.finished(cancelled));
                });
    }

    public CompletionStage<TransitionResult> submit(FSMContext fsm, FormDefinition definition, String input) {
        return storage.get(Objects.requireNonNull(fsm, "fsm"))
                .thenCompose(stateOpt -> {
                    if (stateOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(TransitionResult.missingState());
                    }
                    FormState state = stateOpt.orElseThrow();
                    if (!state.inProgress()) {
                        return CompletableFuture.completedFuture(TransitionResult.blocked(state, "form is not in progress"));
                    }
                    if (!matchesForm(definition, state)) {
                        return CompletableFuture.completedFuture(TransitionResult.blocked(state, "form mismatch"));
                    }
                    if (!definition.isLastStep(state.stepIndex())) {
                        return CompletableFuture.completedFuture(
                                TransitionResult.blocked(state, "submit is available only on last step")
                        );
                    }

                    FormStep step = definition.step(state.stepIndex());
                    FormValidator.ValidationResult validation = step.validator().validate(input, state);
                    if (!validation.valid()) {
                        return CompletableFuture.completedFuture(TransitionResult.blocked(state, validation.error()));
                    }

                    FormState submitted = state.withValue(step.id(), input).submitted();
                    return storage.clear(fsm).thenApply(ignored -> TransitionResult.finished(submitted));
                });
    }

    private static boolean matchesForm(FormDefinition definition, FormState state) {
        return definition != null && definition.id().equals(state.formId());
    }

    public record TransitionResult(FormState state, boolean moved, boolean finished, String error) {
        public TransitionResult {
            if (finished && state == null) {
                throw new IllegalArgumentException("state is required for finished transitions");
            }
            if (moved && state == null) {
                throw new IllegalArgumentException("state is required for moved transitions");
            }
            if (error != null && error.isBlank()) {
                throw new IllegalArgumentException("error must not be blank");
            }
        }

        public boolean blocked() {
            return error != null;
        }

        static TransitionResult moved(FormState state) {
            return new TransitionResult(state, true, false, null);
        }

        static TransitionResult finished(FormState state) {
            return new TransitionResult(state, false, true, null);
        }

        static TransitionResult blocked(FormState state, String error) {
            return new TransitionResult(state, false, false, Objects.requireNonNull(error, "error"));
        }

        static TransitionResult missingState() {
            return new TransitionResult(null, false, false, "form state is missing");
        }
    }
}
