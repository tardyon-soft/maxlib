package ru.tardyon.botframework.screen.form;

import java.util.Objects;

/**
 * Validates user input for one form step.
 */
@FunctionalInterface
public interface FormValidator {

    ValidationResult validate(String input, FormState state);

    static FormValidator none() {
        return (input, state) -> ValidationResult.ok();
    }

    static FormValidator required(String message) {
        Objects.requireNonNull(message, "message");
        return (input, state) -> (input == null || input.isBlank())
                ? ValidationResult.error(message)
                : ValidationResult.ok();
    }

    record ValidationResult(boolean valid, String error) {
        public ValidationResult {
            if (valid && error != null) {
                throw new IllegalArgumentException("error must be null for valid result");
            }
            if (!valid && (error == null || error.isBlank())) {
                throw new IllegalArgumentException("error must not be blank for invalid result");
            }
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String error) {
            return new ValidationResult(false, Objects.requireNonNull(error, "error"));
        }
    }
}

