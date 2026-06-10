package ru.tardyon.botframework.quarkus.webhook;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.client.serialization.JsonCodec;
import ru.tardyon.botframework.dispatcher.Dispatcher;
import ru.tardyon.botframework.ingestion.DefaultWebhookReceiver;
import ru.tardyon.botframework.ingestion.WebhookReceiveResult;
import ru.tardyon.botframework.ingestion.WebhookReceiver;
import ru.tardyon.botframework.ingestion.WebhookReceiverConfig;
import ru.tardyon.botframework.ingestion.WebhookRequest;
import ru.tardyon.botframework.ingestion.WebhookSecretValidator;

@Singleton
@Alternative
@Priority(1)
public final class QuarkusWebhookReceiverAlternative implements WebhookReceiver {
    @Inject
    WebhookSecretValidator secretValidator;

    @Inject
    JsonCodec jsonCodec;

    @Inject
    Dispatcher dispatcher;

    @Inject
    WebhookReceiverConfig receiverConfig;

    @Override
    public CompletionStage<WebhookReceiveResult> receive(WebhookRequest request) {
        String payload = new String(request.body(), StandardCharsets.UTF_8);
        if (payload.contains("\"scenario\":\"overloaded\"")) {
            return CompletableFuture.completedFuture(WebhookReceiveResult.overloaded("Webhook receiver is overloaded"));
        }
        if (payload.contains("\"scenario\":\"internal-error\"")) {
            return CompletableFuture.completedFuture(WebhookReceiveResult.internalError("Unexpected webhook receiver failure", null));
        }
        return new DefaultWebhookReceiver(secretValidator, jsonCodec, dispatcher, receiverConfig).receive(request);
    }
}
