package ru.tardyon.botframework.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.tardyon.botframework.client.serialization.JacksonJsonCodec;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UpdateId;
import ru.tardyon.botframework.model.UpdateType;

class UnifiedIngestionPipelineUsageTest {

    @Test
    void longPollingRunnerUsesUnifiedPipelineContract() throws Exception {
        PollingUpdateSource source = Mockito.mock(PollingUpdateSource.class);
        UpdatePipeline pipeline = Mockito.mock(UpdatePipeline.class);
        CountDownLatch polled = new CountDownLatch(1);

        when(source.poll(any())).thenAnswer(invocation -> {
            polled.countDown();
            return new PollingBatch(List.of(sampleUpdate("u-1")), 10L);
        });
        when(pipeline.process(any(), eq(UpdatePipelineContext.POLLING)))
                .thenReturn(CompletableFuture.completedFuture(UpdatePipelineResult.accepted()));

        DefaultLongPollingRunner runner = new DefaultLongPollingRunner(
                source,
                pipeline,
                LongPollingRunnerConfig.builder().idleDelay(Duration.ofMillis(10)).build()
        );
        runner.start();

        assertTrue(polled.await(1, TimeUnit.SECONDS));
        runner.stop();

        verify(source, atLeastOnce()).poll(any());
        verify(pipeline, atLeastOnce()).process(any(), eq(UpdatePipelineContext.POLLING));
    }

    @Test
    void webhookReceiverUsesUnifiedPipelineContract() {
        UpdatePipeline pipeline = Mockito.mock(UpdatePipeline.class);
        when(pipeline.process(any(), eq(UpdatePipelineContext.WEBHOOK)))
                .thenReturn(CompletableFuture.completedFuture(UpdatePipelineResult.accepted()));
        DefaultWebhookReceiver receiver = new DefaultWebhookReceiver(
                new DefaultWebhookSecretValidator("secret-1"),
                new JacksonJsonCodec(),
                pipeline
        );

        WebhookReceiveResult result = receiver.receive(new WebhookRequest(
                validPayload().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                Map.of(DefaultWebhookSecretValidator.SECRET_HEADER_NAME, List.of("secret-1"))
        )).toCompletableFuture().join();

        assertEquals(WebhookReceiveStatus.ACCEPTED, result.status());
        verify(pipeline).process(any(), eq(UpdatePipelineContext.WEBHOOK));
    }

    private static String validPayload() {
        return """
                {
                  "updateId": "upd-1",
                  "type": "message"
                }
                """;
    }

    private static Update sampleUpdate(String id) {
        return new Update(
                new UpdateId(id),
                UpdateType.MESSAGE,
                null,
                null,
                null,
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }
}
