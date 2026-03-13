package ru.tardyon.botframework.ingestion;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tardyon.botframework.client.serialization.JacksonJsonCodec;
import ru.tardyon.botframework.model.Update;

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

    @Test
    void returnsOverloadedWhenInFlightLimitReached() throws Exception {
        UpdatePipeline pipeline = Mockito.mock(UpdatePipeline.class);
        CompletableFuture<UpdatePipelineResult> firstRequestCompletion = new CompletableFuture<>();
        CountDownLatch firstAccepted = new CountDownLatch(1);
        when(pipeline.process(any(), any())).thenAnswer(invocation -> {
            firstAccepted.countDown();
            return firstRequestCompletion;
        });
        DefaultWebhookReceiver receiver = new DefaultWebhookReceiver(
                new DefaultWebhookSecretValidator("secret-1"),
                new JacksonJsonCodec(),
                pipeline,
                new WebhookReceiverConfig(1)
        );

        CompletableFuture<WebhookReceiveResult> first = receiver.receive(request(validPayload(), "secret-1"))
                .toCompletableFuture();
        assertTrue(firstAccepted.await(1, TimeUnit.SECONDS));

        WebhookReceiveResult overloaded = receiver.receive(request(validPayload(), "secret-1"))
                .toCompletableFuture()
                .join();
        assertEquals(WebhookReceiveStatus.OVERLOADED, overloaded.status());

        firstRequestCompletion.complete(UpdatePipelineResult.accepted());
        WebhookReceiveResult firstResult = first.join();
        assertEquals(WebhookReceiveStatus.ACCEPTED, firstResult.status());
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
