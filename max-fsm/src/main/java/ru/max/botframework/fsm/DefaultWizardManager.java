package ru.max.botframework.fsm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
        WizardStep first = wizard.firstStep().orElseThrow(() -> new IllegalStateException("wizard has no steps"));

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
            return fsm.data().thenApply(data -> readStepIndex(data).flatMap(wizard::stepAt));
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
            Wizard wizard = wizardOpt.orElseThrow(() -> new IllegalStateException("wizard is not active"));
            return fsm.data().thenCompose(data -> {
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
        throw new IllegalStateException("scene '%s' is not a wizard".formatted(scene.id()));
    }

    private CompletionStage<Void> persistStep(int index, WizardStep step) {
        return fsm.updateData(Map.of(
                STEP_INDEX_KEY, index,
                STEP_ID_KEY, step.id()
        )).thenApply(ignored -> null);
    }

    private CompletionStage<Void> clearWizardMetadata() {
        return fsm.data().thenCompose(data -> {
            if (data.values().isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            LinkedHashMap<String, Object> kept = new LinkedHashMap<>(data.values());
            kept.remove(STEP_INDEX_KEY);
            kept.remove(STEP_ID_KEY);
            return fsm.setData(StateData.of(kept));
        });
    }

    private static Optional<Integer> readStepIndex(StateData data) {
        return data.get(STEP_INDEX_KEY, Integer.class);
    }
}
