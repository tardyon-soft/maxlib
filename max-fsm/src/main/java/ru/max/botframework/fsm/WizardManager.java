package ru.max.botframework.fsm;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Runtime wizard flow controller built on SceneManager + FSMContext.
 */
public interface WizardManager {

    CompletionStage<Void> enter(String wizardId);

    CompletionStage<Optional<WizardStep>> currentStep();

    CompletionStage<Void> next();

    CompletionStage<Void> back();

    CompletionStage<Void> exit();
}
