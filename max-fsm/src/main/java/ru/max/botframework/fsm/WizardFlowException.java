package ru.max.botframework.fsm;

/**
 * Signals invalid wizard flow usage or inconsistent wizard state.
 */
public final class WizardFlowException extends IllegalStateException {

    private WizardFlowException(String message) {
        super(message);
    }

    static WizardFlowException noSteps(String wizardId) {
        return new WizardFlowException("wizard '%s' has no steps".formatted(wizardId));
    }

    static WizardFlowException notActive() {
        return new WizardFlowException("wizard is not active");
    }

    static WizardFlowException sceneIsNotWizard(String sceneId) {
        return new WizardFlowException("scene '%s' is not a wizard".formatted(sceneId));
    }
}
