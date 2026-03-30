package ru.tardyon.botframework.screen.form;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Form contract with ordered steps.
 */
public record FormDefinition(
        String id,
        List<FormStep> steps
) {
    public FormDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(steps, "steps");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("steps must not be empty");
        }
        steps = List.copyOf(steps);

        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (FormStep step : steps) {
            Objects.requireNonNull(step, "step");
            if (!seen.add(step.id())) {
                throw new IllegalArgumentException("duplicate step id: " + step.id());
            }
        }
    }

    public static FormDefinition of(String id, FormStep... steps) {
        Objects.requireNonNull(steps, "steps");
        return new FormDefinition(id, List.of(steps));
    }

    public FormStep step(int index) {
        if (index < 0 || index >= steps.size()) {
            throw new IllegalArgumentException("step index is out of bounds: " + index);
        }
        return steps.get(index);
    }

    public boolean isLastStep(int index) {
        if (index < 0 || index >= steps.size()) {
            throw new IllegalArgumentException("step index is out of bounds: " + index);
        }
        return index == steps.size() - 1;
    }
}
