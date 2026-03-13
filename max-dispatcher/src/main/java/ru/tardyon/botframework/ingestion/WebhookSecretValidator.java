package ru.tardyon.botframework.ingestion;

/**
 * Contract for webhook secret validation.
 */
public interface WebhookSecretValidator {

    WebhookSecretValidationResult validate(String secretHeader);
}
