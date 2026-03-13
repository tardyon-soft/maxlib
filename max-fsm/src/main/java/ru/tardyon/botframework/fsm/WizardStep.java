package ru.tardyon.botframework.fsm;

import java.util.Objects;

/**
 * Wizard step identity.
 */
public record WizardStep(String id) {
    public WizardStep {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("step id must not be blank");
        }
    }
}
