package ru.tardyon.botframework.screen.form;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime form snapshot.
 */
public record FormState(
        String formId,
        int stepIndex,
        Map<String, String> values,
        Status status
) {
    public FormState {
        Objects.requireNonNull(formId, "formId");
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(status, "status");
        if (formId.isBlank()) {
            throw new IllegalArgumentException("formId must not be blank");
        }
        if (stepIndex < 0) {
            throw new IllegalArgumentException("stepIndex must be >= 0");
        }
        values = Map.copyOf(values);
    }

    public static FormState start(String formId) {
        return new FormState(formId, 0, Map.of(), Status.IN_PROGRESS);
    }

    public boolean inProgress() {
        return status == Status.IN_PROGRESS;
    }

    public FormState withStepIndex(int nextStepIndex) {
        return new FormState(formId, nextStepIndex, values, status);
    }

    public FormState withValue(String key, String value) {
        Objects.requireNonNull(key, "key");
        LinkedHashMap<String, String> next = new LinkedHashMap<>(values);
        next.put(key, value == null ? "" : value);
        return new FormState(formId, stepIndex, next, status);
    }

    public FormState submitted() {
        return new FormState(formId, stepIndex, values, Status.SUBMITTED);
    }

    public FormState cancelled() {
        return new FormState(formId, stepIndex, values, Status.CANCELLED);
    }

    public enum Status {
        IN_PROGRESS,
        SUBMITTED,
        CANCELLED
    }
}
