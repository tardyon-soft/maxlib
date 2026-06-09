package ru.tardyon.botframework.micronaut.webhook;

import io.micronaut.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.ingestion.WebhookReceiveResult;
import ru.tardyon.botframework.ingestion.WebhookReceiver;
import ru.tardyon.botframework.ingestion.WebhookRequest;

/**
 * Framework-agnostic webhook adapter bridge for Micronaut web layer.
 */
public final class MicronautWebhookAdapter {
    private final WebhookReceiver webhookReceiver;

    public MicronautWebhookAdapter(WebhookReceiver webhookReceiver) {
        this.webhookReceiver = Objects.requireNonNull(webhookReceiver, "webhookReceiver");
    }

    public CompletionStage<WebhookReceiveResult> receive(byte[] body, Map<String, List<String>> headers) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(headers, "headers");
        return webhookReceiver.receive(new WebhookRequest(body, headers));
    }

    public CompletionStage<WebhookReceiveResult> receive(byte[] body, HttpHeaders headers) {
        Objects.requireNonNull(headers, "headers");
        return receive(body, copyHeaders(headers));
    }

    public CompletionStage<WebhookReceiveResult> receive(String body, Map<String, List<String>> headers) {
        Objects.requireNonNull(body, "body");
        return receive(body.getBytes(StandardCharsets.UTF_8), headers);
    }

    public CompletionStage<WebhookReceiveResult> receive(String body, HttpHeaders headers) {
        Objects.requireNonNull(body, "body");
        return receive(body.getBytes(StandardCharsets.UTF_8), headers);
    }

    private static Map<String, List<String>> copyHeaders(HttpHeaders headers) {
        Map<String, List<String>> copied = new LinkedHashMap<>();
        for (String name : headers.names()) {
            copied.put(name, List.copyOf(headers.getAll(name)));
        }
        return copied;
    }
}
