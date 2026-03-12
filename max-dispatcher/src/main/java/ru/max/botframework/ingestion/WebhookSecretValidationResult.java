package ru.max.botframework.ingestion;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of webhook secret validation.
 */
public record WebhookSecretValidationResult(
        WebhookSecretValidationStatus status,
        WebhookValidationError validationError
) {
    public WebhookSecretValidationResult {
        Objects.requireNonNull(status, "status");
    }

    public static WebhookSecretValidationResult accepted() {
        return new WebhookSecretValidationResult(WebhookSecretValidationStatus.ACCEPTED, null);
    }

    public static WebhookSecretValidationResult skippedNoSecretConfigured() {
        return new WebhookSecretValidationResult(WebhookSecretValidationStatus.SKIPPED_NO_SECRET_CONFIGURED, null);
    }

    public static WebhookSecretValidationResult rejected(WebhookValidationErrorCode code, String message) {
        return new WebhookSecretValidationResult(
                WebhookSecretValidationStatus.REJECTED,
                new WebhookValidationError(code, message)
        );
    }

    public boolean isAccepted() {
        return status != WebhookSecretValidationStatus.REJECTED;
    }

    public Optional<WebhookValidationError> error() {
        return Optional.ofNullable(validationError);
    }
}
