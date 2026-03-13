package ru.max.botframework.fsm;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Scene with ordered steps for linear wizard flows.
 */
public interface Wizard extends Scene {

    List<WizardStep> steps();

    default Optional<WizardStep> firstStep() {
        List<WizardStep> steps = steps();
        return steps.isEmpty() ? Optional.empty() : Optional.of(steps.getFirst());
    }

    default Optional<WizardStep> stepAt(int index) {
        List<WizardStep> steps = steps();
        if (index < 0 || index >= steps.size()) {
            return Optional.empty();
        }
        return Optional.of(steps.get(index));
    }

    static Builder named(String id) {
        return new Builder(id);
    }

    final class Builder {
        private final String id;
        private final java.util.ArrayList<WizardStep> steps = new java.util.ArrayList<>();

        private Builder(String id) {
            Objects.requireNonNull(id, "id");
            if (id.isBlank()) {
                throw new IllegalArgumentException("wizard id must not be blank");
            }
            this.id = id;
        }

        public Builder step(String stepId) {
            WizardStep step = new WizardStep(stepId);
            if (steps.stream().anyMatch(existing -> existing.id().equals(step.id()))) {
                throw new IllegalStateException("duplicate wizard step '%s'".formatted(step.id()));
            }
            steps.add(step);
            return this;
        }

        public Wizard build() {
            if (steps.isEmpty()) {
                throw new IllegalStateException("wizard requires at least one step");
            }
            return new DefaultWizard(id, List.copyOf(steps));
        }
    }
}
