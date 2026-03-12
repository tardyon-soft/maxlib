package ru.max.botframework.ingestion;

/**
 * Boundary adapter between webhook HTTP ingress and ingestion source.
 */
public interface WebhookReceiver {

    void receive(byte[] payload, UpdateSink sink);
}
