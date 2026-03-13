package ru.tardyon.botframework.examples.sprint2;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import ru.tardyon.botframework.client.serialization.JacksonJsonCodec;
import ru.tardyon.botframework.ingestion.DefaultWebhookReceiver;
import ru.tardyon.botframework.ingestion.DefaultWebhookSecretValidator;
import ru.tardyon.botframework.ingestion.UpdateHandlingResult;
import ru.tardyon.botframework.ingestion.UpdateConsumer;
import ru.tardyon.botframework.ingestion.WebhookReceiveResult;
import ru.tardyon.botframework.ingestion.WebhookReceiveStatus;
import ru.tardyon.botframework.ingestion.WebhookReceiverConfig;
import ru.tardyon.botframework.ingestion.WebhookRequest;

public final class WebhookHandlingExample {

    private final DefaultWebhookReceiver receiver;

    public WebhookHandlingExample() {
        UpdateConsumer sink = update -> {
            System.out.println("Webhook update " + update.updateId().value());
            return CompletableFuture.completedFuture(UpdateHandlingResult.success());
        };

        this.receiver = new DefaultWebhookReceiver(
                new DefaultWebhookSecretValidator("my-webhook-secret"),
                new JacksonJsonCodec(),
                sink,
                new WebhookReceiverConfig(128)
        );
    }

    public int handle(byte[] body, String secretHeader) {
        WebhookRequest request = new WebhookRequest(
                body,
                Map.of(DefaultWebhookSecretValidator.SECRET_HEADER_NAME, List.of(secretHeader))
        );

        WebhookReceiveResult result = receiver.receive(request).toCompletableFuture().join();
        return toHttpStatus(result.status());
    }

    private static int toHttpStatus(WebhookReceiveStatus status) {
        return switch (status) {
            case ACCEPTED -> 200;
            case INVALID_SECRET -> 403;
            case BAD_PAYLOAD -> 400;
            case OVERLOADED -> 429;
            case INTERNAL_ERROR -> 500;
        };
    }
}
