package ru.tardyon.botframework.fsm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * Default wizard flow manager.
 */
public final class DefaultWizardManager implements WizardManager {

    private static final String STEP_INDEX_KEY = "wizard.stepIndex";
    private static final String STEP_ID_KEY = "wizard.stepId";

    private final SceneRegistry registry;
    private final SceneManager scenes;
    private final FSMContext fsm;

    public DefaultWizardManager(SceneRegistry registry, SceneManager scenes, FSMContext fsm) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.scenes = Objects.requireNonNull(scenes, "scenes");
        this.fsm = Objects.requireNonNull(fsm, "fsm");
    }

    @Override
    public CompletionStage<Void> enter(String wizardId) {
        Wizard wizard = requireWizard(wizardId);
        WizardStep first = wizard.firstStep().orElseThrow(() -> WizardFlowException.noSteps(wizard.id()));

        return scenes.enter(wizard.id())
                .thenCompose(ignored -> persistStep(0, first));
    }

    @Override
    public CompletionStage<Optional<WizardStep>> currentStep() {
        return currentWizard().thenCompose(wizardOpt -> {
            if (wizardOpt.isEmpty()) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            Wizard wizard = wizardOpt.orElseThrow();
            return wrapStorageFailure(fsm.data(), "fsm.data")
                    .thenApply(data -> readStepIndex(data).flatMap(wizard::stepAt));
        });
    }

    @Override
    public CompletionStage<Void> next() {
        return move(1);
    }

    @Override
    public CompletionStage<Void> back() {
        return move(-1);
    }

    @Override
    public CompletionStage<Void> exit() {
        return scenes.exit().thenCompose(ignored -> clearWizardMetadata());
    }

    private CompletionStage<Void> move(int delta) {
        return currentWizard().thenCompose(wizardOpt -> {
            Wizard wizard = wizardOpt.orElseThrow(WizardFlowException::notActive);
            return wrapStorageFailure(fsm.data(), "fsm.data").thenCompose(data -> {
                int current = readStepIndex(data).orElse(0);
                int target = Math.max(0, Math.min(wizard.steps().size() - 1, current + delta));
                if (target == current) {
                    return CompletableFuture.completedFuture(null);
                }
                WizardStep step = wizard.stepAt(target).orElseThrow();
                return persistStep(target, step);
            });
        });
    }

    private CompletionStage<Optional<Wizard>> currentWizard() {
        return scenes.currentScene().thenApply(session -> session.flatMap(scene -> registry.find(scene.sceneId()))
                .map(this::asWizard));
    }

    private Wizard requireWizard(String wizardId) {
        Scene scene = registry.find(Objects.requireNonNull(wizardId, "wizardId"))
                .orElseThrow(() -> new SceneNotFoundException(wizardId));
        return asWizard(scene);
    }

    private Wizard asWizard(Scene scene) {
        if (scene instanceof Wizard wizard) {
            return wizard;
        }
        throw WizardFlowException.sceneIsNotWizard(scene.id());
    }

    private CompletionStage<Void> persistStep(int index, WizardStep step) {
        return wrapStorageFailure(fsm.updateData(Map.of(
                STEP_INDEX_KEY, index,
                STEP_ID_KEY, step.id()
        )), "fsm.updateData").thenApply(ignored -> null);
    }

    private CompletionStage<Void> clearWizardMetadata() {
        return wrapStorageFailure(fsm.data(), "fsm.data").thenCompose(data -> {
            if (data.values().isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            LinkedHashMap<String, Object> kept = new LinkedHashMap<>(data.values());
            kept.remove(STEP_INDEX_KEY);
            kept.remove(STEP_ID_KEY);
            return wrapStorageFailure(fsm.setData(StateData.of(kept)), "fsm.setData");
        });
    }

    private static Optional<Integer> readStepIndex(StateData data) {
        return data.get(STEP_INDEX_KEY, Integer.class);
    }

    private static <T> CompletionStage<T> wrapStorageFailure(CompletionStage<T> stage, String operation) {
        return stage.handle((value, throwable) -> {
            if (throwable == null) {
                return CompletableFuture.completedFuture(value);
            }
            Throwable cause = unwrap(throwable);
            return CompletableFuture.<T>failedFuture(new FsmStorageException(operation, cause));
        }).thenCompose(next -> next);
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException completion && completion.getCause() != null) {
            return completion.getCause();
        }
        return throwable;
    }
}
