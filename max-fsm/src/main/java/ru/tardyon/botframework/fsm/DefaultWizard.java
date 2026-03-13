package ru.tardyon.botframework.fsm;

import java.util.List;
import java.util.Objects;

/**
 * Immutable wizard definition.
 */
public record DefaultWizard(String id, List<WizardStep> steps) implements Wizard {
    public DefaultWizard {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(steps, "steps");
        if (id.isBlank()) {
            throw new IllegalArgumentException("wizard id must not be blank");
        }
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("wizard requires at least one step");
        }
        steps = List.copyOf(steps);
    }
}
