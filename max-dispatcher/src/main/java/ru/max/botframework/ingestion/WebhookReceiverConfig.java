package ru.max.botframework.ingestion;

/**
 * Config for framework-agnostic webhook receiver.
 */
public record WebhookReceiverConfig(int maxInFlightRequests) {
    public static final int DEFAULT_MAX_IN_FLIGHT_REQUESTS = 256;

    public WebhookReceiverConfig {
        if (maxInFlightRequests <= 0) {
            throw new IllegalArgumentException("maxInFlightRequests must be positive");
        }
    }

    public static WebhookReceiverConfig defaults() {
        return new WebhookReceiverConfig(DEFAULT_MAX_IN_FLIGHT_REQUESTS);
    }
}
