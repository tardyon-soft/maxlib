package ru.tardyon.botframework.screen.form;

import java.util.Objects;

/**
 * One logical step of a form.
 */
public record FormStep(
        String id,
        String prompt,
        FormValidator validator
) {
    public FormStep {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(prompt, "prompt");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        validator = validator == null ? FormValidator.none() : validator;
    }

    public static FormStep of(String id, String prompt) {
        return new FormStep(id, prompt, FormValidator.none());
    }

    public static FormStep of(String id, String prompt, FormValidator validator) {
        return new FormStep(id, prompt, validator);
    }
}
