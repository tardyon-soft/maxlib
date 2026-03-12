package ru.max.botframework.ingestion;

/**
 * Secret validation outcome status for incoming webhook requests.
 */
public enum WebhookSecretValidationStatus {
    ACCEPTED,
    SKIPPED_NO_SECRET_CONFIGURED,
    REJECTED
}
