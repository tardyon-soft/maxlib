package ru.tardyon.botframework.fsm;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Runtime wizard flow controller built on SceneManager + FSMContext.
 *
 * <p>Wizard manager is a lightweight helper over scene/FSM primitives and does not
 * implement complex workflow branching.</p>
 */
public interface WizardManager {

    /**
     * Enters wizard scene and initializes first step metadata.
     */
    CompletionStage<Void> enter(String wizardId);

    /**
     * Returns current wizard step or empty when wizard is inactive.
     */
    CompletionStage<Optional<WizardStep>> currentStep();

    /**
     * Moves wizard one step forward.
     */
    CompletionStage<Void> next();

    /**
     * Moves wizard one step backward.
     */
    CompletionStage<Void> back();

    /**
     * Exits wizard scene and clears wizard-specific metadata.
     */
    CompletionStage<Void> exit();
}
