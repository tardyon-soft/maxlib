package ru.max.botframework.ingestion;

import java.util.concurrent.CompletionStage;

/**
 * Boundary adapter between webhook HTTP ingress and ingestion source.
 */
public interface WebhookReceiver {

    CompletionStage<WebhookReceiveResult> receive(WebhookRequest request);
}
