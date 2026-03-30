package ru.tardyon.botframework.screen.form;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.fsm.FSMContext;
import ru.tardyon.botframework.fsm.StateData;

/**
 * Form state storage backed by FSM payload data.
 */
public final class FsmFormStateStorage implements FormStateStorage {
    static final String FORM_STATE_KEY = "ui.form.state.v1";

    @Override
    public CompletionStage<Optional<FormState>> get(FSMContext fsm) {
        Objects.requireNonNull(fsm, "fsm");
        return fsm.data().thenApply(data -> decode(data.get(FORM_STATE_KEY).orElse(null)));
    }

    @Override
    public CompletionStage<Void> set(FSMContext fsm, FormState state) {
        Objects.requireNonNull(fsm, "fsm");
        Objects.requireNonNull(state, "state");
        return fsm.updateData(Map.of(FORM_STATE_KEY, encode(state))).thenAccept(ignored -> {
        });
    }

    @Override
    public CompletionStage<Void> clear(FSMContext fsm) {
        Objects.requireNonNull(fsm, "fsm");
        return fsm.data().thenCompose(existing -> {
            LinkedHashMap<String, Object> values = new LinkedHashMap<>(existing.values());
            values.remove(FORM_STATE_KEY);
            return fsm.setData(StateData.of(values));
        });
    }

    private static Map<String, Object> encode(FormState state) {
        return Map.of(
                "formId", state.formId(),
                "stepIndex", state.stepIndex(),
                "values", new LinkedHashMap<>(state.values()),
                "status", state.status().name()
        );
    }

    private static Optional<FormState> decode(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Optional.empty();
        }

        Object formIdRaw = map.get("formId");
        Object stepIndexRaw = map.get("stepIndex");
        Object valuesRaw = map.get("values");
        Object statusRaw = map.get("status");

        if (!(formIdRaw instanceof String formId) || formId.isBlank()) {
            return Optional.empty();
        }
        if (!(stepIndexRaw instanceof Number stepNumber)) {
            return Optional.empty();
        }
        int stepIndex = stepNumber.intValue();
        if (stepIndex < 0) {
            return Optional.empty();
        }
        if (!(statusRaw instanceof String statusName)) {
            return Optional.empty();
        }

        FormState.Status status;
        try {
            status = FormState.Status.valueOf(statusName);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }

        Map<String, String> values = decodeValues(valuesRaw);
        if (values == null) {
            return Optional.empty();
        }

        return Optional.of(new FormState(formId, stepIndex, values, status));
    }

    private static Map<String, String> decodeValues(Object raw) {
        if (!(raw instanceof Map<?, ?> valuesMap)) {
            return Map.of();
        }
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : valuesMap.entrySet()) {
            if (!(entry.getKey() instanceof String key) || key.isBlank()) {
                return null;
            }
            Object rawValue = entry.getValue();
            values.put(key, rawValue == null ? "" : String.valueOf(rawValue));
        }
        return Map.copyOf(values);
    }
}
