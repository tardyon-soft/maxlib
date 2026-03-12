package ru.max.botframework.ingestion;

import java.util.concurrent.CompletionStage;

/**
 * Framework-agnostic boundary between webhook HTTP ingress and ingestion flow.
 */
public interface WebhookReceiver {

    /**
     * Handles a raw webhook request and returns transport-level ingestion outcome.
     */
    CompletionStage<WebhookReceiveResult> receive(WebhookRequest request);
}
