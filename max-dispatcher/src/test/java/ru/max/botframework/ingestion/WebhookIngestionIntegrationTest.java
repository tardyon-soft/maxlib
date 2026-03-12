package ru.max.botframework.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import ru.max.botframework.client.serialization.JacksonJsonCodec;
import ru.max.botframework.model.Update;

class WebhookIngestionIntegrationTest {

    @Test
    void validWebhookFixtureFlowsThroughValidationReceiverAndSink() {
        List<Update> received = new CopyOnWriteArrayList<>();
        UpdateSink sink = update -> {
            received.add(update);
            return CompletableFuture.completedFuture(UpdateHandlingResult.success());
        };
        DefaultWebhookReceiver receiver = receiver(sink);

        WebhookReceiveResult result = receiver.receive(webhookRequest("webhook-valid-payload.json", "secret-1"))
                .toCompletableFuture()
                .join();

        assertEquals(WebhookReceiveStatus.ACCEPTED, result.status());
        assertTrue(result.isAccepted());
        assertEquals(1, received.size());
        assertEquals("upd-20", received.get(0).updateId().value());
    }

    @Test
    void malformedWebhookFixtureReturnsBadPayload() {
        UpdateSink sink = update -> CompletableFuture.completedFuture(UpdateHandlingResult.success());
        DefaultWebhookReceiver receiver = receiver(sink);

        WebhookReceiveResult result = receiver.receive(webhookRequest("webhook-invalid-payload.json", "secret-1"))
                .toCompletableFuture()
                .join();

        assertEquals(WebhookReceiveStatus.BAD_PAYLOAD, result.status());
        assertFalse(result.isAccepted());
    }

    @Test
    void sinkFailureIsReturnedAsInternalError() {
        UpdateSink sink = update -> CompletableFuture.completedFuture(
                UpdateHandlingResult.failure(new IllegalStateException("sink failure"))
        );
        DefaultWebhookReceiver receiver = receiver(sink);

        WebhookReceiveResult result = receiver.receive(webhookRequest("webhook-valid-payload.json", "secret-1"))
                .toCompletableFuture()
                .join();

        assertEquals(WebhookReceiveStatus.INTERNAL_ERROR, result.status());
        assertFalse(result.isAccepted());
    }

    private static DefaultWebhookReceiver receiver(UpdateSink sink) {
        return new DefaultWebhookReceiver(
                new DefaultWebhookSecretValidator("secret-1"),
                new JacksonJsonCodec(),
                sink
        );
    }

    private static WebhookRequest webhookRequest(String fixture, String secret) {
        return new WebhookRequest(
                IngestionFixtures.raw(fixture).getBytes(StandardCharsets.UTF_8),
                Map.of(DefaultWebhookSecretValidator.SECRET_HEADER_NAME, List.of(secret))
        );
    }
}
