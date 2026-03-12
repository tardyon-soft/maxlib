package ru.max.botframework.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.max.botframework.client.serialization.JacksonJsonCodec;
import ru.max.botframework.model.Update;

class DefaultWebhookReceiverTest {

    @Test
    void validRequestIsAcceptedAndForwardedToSink() {
        UpdateSink sink = Mockito.mock(UpdateSink.class);
        when(sink.handle(any())).thenReturn(CompletableFuture.completedFuture(UpdateHandlingResult.success()));

        DefaultWebhookReceiver receiver = new DefaultWebhookReceiver(
                new DefaultWebhookSecretValidator("secret-1"),
                new JacksonJsonCodec(),
                sink
        );

        WebhookReceiveResult result = receiver.receive(request(validPayload(), "secret-1")).toCompletableFuture().join();

        assertEquals(WebhookReceiveStatus.ACCEPTED, result.status());
        assertTrue(result.isAccepted());
        assertTrue(result.secretValidation() != null);
        assertEquals(WebhookSecretValidationStatus.ACCEPTED, result.secretValidation().status());
        verify(sink).handle(any(Update.class));
    }

    @Test
    void invalidSecretReturnsInvalidSecretResult() {
        UpdateSink sink = Mockito.mock(UpdateSink.class);
        DefaultWebhookReceiver receiver = new DefaultWebhookReceiver(
                new DefaultWebhookSecretValidator("secret-1"),
                new JacksonJsonCodec(),
                sink
        );

        WebhookReceiveResult result = receiver.receive(request(validPayload(), "wrong")).toCompletableFuture().join();

        assertEquals(WebhookReceiveStatus.INVALID_SECRET, result.status());
        assertFalse(result.isAccepted());
        assertEquals(
                WebhookValidationErrorCode.SECRET_MISMATCH,
                result.secretValidation().error().orElseThrow().code()
        );
        verify(sink, never()).handle(any());
    }

    @Test
    void malformedPayloadReturnsBadPayloadResult() {
        UpdateSink sink = Mockito.mock(UpdateSink.class);
        DefaultWebhookReceiver receiver = new DefaultWebhookReceiver(
                new DefaultWebhookSecretValidator("secret-1"),
                new JacksonJsonCodec(),
                sink
        );

        WebhookReceiveResult result = receiver.receive(request("{", "secret-1")).toCompletableFuture().join();

        assertEquals(WebhookReceiveStatus.BAD_PAYLOAD, result.status());
        assertFalse(result.isAccepted());
        verify(sink, never()).handle(any());
    }

    @Test
    void sinkFailureReturnsInternalError() {
        UpdateSink sink = Mockito.mock(UpdateSink.class);
        when(sink.handle(any())).thenReturn(
                CompletableFuture.completedFuture(UpdateHandlingResult.failure(new RuntimeException("sink failed")))
        );
        DefaultWebhookReceiver receiver = new DefaultWebhookReceiver(
                new DefaultWebhookSecretValidator("secret-1"),
                new JacksonJsonCodec(),
                sink
        );

        WebhookReceiveResult result = receiver.receive(request(validPayload(), "secret-1")).toCompletableFuture().join();

        assertEquals(WebhookReceiveStatus.INTERNAL_ERROR, result.status());
        assertFalse(result.isAccepted());
    }

    private static WebhookRequest request(String body, String secretHeader) {
        return new WebhookRequest(
                body.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                Map.of(DefaultWebhookSecretValidator.SECRET_HEADER_NAME, List.of(secretHeader))
        );
    }

    private static String validPayload() {
        return """
                {
                  "updateId": "upd-1",
                  "type": "message"
                }
                """;
    }
}
