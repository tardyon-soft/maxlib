package ru.tardyon.botframework.ingestion;

import java.util.Objects;

/**
 * Structured webhook validation error contract.
 */
public record WebhookValidationError(
        WebhookValidationErrorCode code,
        String message
) {
    public WebhookValidationError {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }
}
