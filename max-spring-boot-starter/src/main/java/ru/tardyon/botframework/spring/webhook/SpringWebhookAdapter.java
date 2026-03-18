package ru.tardyon.botframework.spring.webhook;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tardyon.botframework.ingestion.WebhookReceiveResult;
import ru.tardyon.botframework.ingestion.WebhookReceiver;
import ru.tardyon.botframework.ingestion.WebhookRequest;

/**
 * Framework-agnostic webhook adapter bridge for Spring web layer.
 */
public final class SpringWebhookAdapter {
    private static final Logger log = LoggerFactory.getLogger(SpringWebhookAdapter.class);
    private final WebhookReceiver webhookReceiver;

    public SpringWebhookAdapter(WebhookReceiver webhookReceiver) {
        this.webhookReceiver = Objects.requireNonNull(webhookReceiver, "webhookReceiver");
    }

    public CompletionStage<WebhookReceiveResult> receive(byte[] body, Map<String, List<String>> headers) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(headers, "headers");
        log.debug("SpringWebhookAdapter.receive invoked: bodyBytes={}, headerKeys={}", body.length, headers.keySet());
        return webhookReceiver.receive(new WebhookRequest(body, headers));
    }

    public CompletionStage<WebhookReceiveResult> receive(String body, Map<String, List<String>> headers) {
        Objects.requireNonNull(body, "body");
        return receive(body.getBytes(StandardCharsets.UTF_8), headers);
    }
}
