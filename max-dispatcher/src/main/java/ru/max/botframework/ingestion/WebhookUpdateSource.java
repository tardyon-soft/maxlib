package ru.max.botframework.ingestion;

/**
 * Reserved transport source abstraction for webhook runtime implementations.
 *
 * <p>Current webhook ingress entrypoint is {@link WebhookReceiver}.</p>
 */
@Deprecated(forRemoval = false)
public interface WebhookUpdateSource extends UpdateSource {
}
