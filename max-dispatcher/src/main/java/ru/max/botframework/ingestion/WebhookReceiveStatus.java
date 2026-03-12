package ru.max.botframework.ingestion;

/**
 * Webhook receiver handling status for web adapters.
 */
public enum WebhookReceiveStatus {
    ACCEPTED,
    INVALID_SECRET,
    OVERLOADED,
    BAD_PAYLOAD,
    INTERNAL_ERROR
}
