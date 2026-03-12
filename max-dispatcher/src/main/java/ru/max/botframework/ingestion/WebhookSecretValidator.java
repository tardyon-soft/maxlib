package ru.max.botframework.ingestion;

/**
 * Contract for webhook secret validation.
 */
public interface WebhookSecretValidator {

    WebhookSecretValidationResult validate(WebhookUpdatePayload payload);
}
