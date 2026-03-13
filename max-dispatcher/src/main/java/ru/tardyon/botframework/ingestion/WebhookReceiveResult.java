package ru.tardyon.botframework.ingestion;

import java.util.Objects;
import java.util.Optional;

/**
 * Receiver outcome that can be mapped to web-layer responses.
 */
public record WebhookReceiveResult(
        WebhookReceiveStatus status,
        WebhookSecretValidationResult secretValidation,
        String message,
        Throwable cause
) {
    public WebhookReceiveResult {
        Objects.requireNonNull(status, "status");
    }

    public static WebhookReceiveResult accepted(WebhookSecretValidationResult validation) {
        return new WebhookReceiveResult(WebhookReceiveStatus.ACCEPTED, validation, null, null);
    }

    public static WebhookReceiveResult invalidSecret(WebhookSecretValidationResult validation) {
        return new WebhookReceiveResult(WebhookReceiveStatus.INVALID_SECRET, validation, "Webhook secret validation failed", null);
    }

    public static WebhookReceiveResult overloaded(String message) {
        return new WebhookReceiveResult(WebhookReceiveStatus.OVERLOADED, null, message, null);
    }

    public static WebhookReceiveResult badPayload(String message, Throwable cause) {
        return new WebhookReceiveResult(WebhookReceiveStatus.BAD_PAYLOAD, null, message, cause);
    }

    public static WebhookReceiveResult internalError(String message, Throwable cause) {
        return new WebhookReceiveResult(WebhookReceiveStatus.INTERNAL_ERROR, null, message, cause);
    }

    public boolean isAccepted() {
        return status == WebhookReceiveStatus.ACCEPTED;
    }

    public Optional<Throwable> causeOptional() {
        return Optional.ofNullable(cause);
    }
}
