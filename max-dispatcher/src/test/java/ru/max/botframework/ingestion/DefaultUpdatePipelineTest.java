package ru.max.botframework.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.max.botframework.model.Update;
import ru.max.botframework.model.UpdateId;
import ru.max.botframework.model.UpdateType;

class DefaultUpdatePipelineTest {

    @Test
    void acceptedWhenSinkReturnsSuccess() {
        UpdateSink sink = Mockito.mock(UpdateSink.class);
        when(sink.handle(Mockito.any())).thenReturn(CompletableFuture.completedFuture(UpdateHandlingResult.success()));
        DefaultUpdatePipeline pipeline = new DefaultUpdatePipeline(sink);

        UpdatePipelineResult result = pipeline.process(sampleUpdate("u-1"), UpdatePipelineContext.POLLING)
                .toCompletableFuture()
                .join();

        assertTrue(result.isAccepted());
        assertEquals(UpdatePipelineStatus.ACCEPTED, result.status());
    }

    @Test
    void rejectedWhenSinkReturnsFailure() {
        UpdateSink sink = Mockito.mock(UpdateSink.class);
        RuntimeException expected = new RuntimeException("sink failed");
        when(sink.handle(Mockito.any()))
                .thenReturn(CompletableFuture.completedFuture(UpdateHandlingResult.failure(expected)));
        DefaultUpdatePipeline pipeline = new DefaultUpdatePipeline(sink);

        UpdatePipelineResult result = pipeline.process(sampleUpdate("u-1"), UpdatePipelineContext.POLLING)
                .toCompletableFuture()
                .join();

        assertFalse(result.isAccepted());
        assertEquals(UpdatePipelineStatus.REJECTED, result.status());
        assertEquals(expected, result.error().orElseThrow());
    }

    @Test
    void hooksAreCalledForBeforeAndAfter() {
        UpdateSink sink = Mockito.mock(UpdateSink.class);
        when(sink.handle(Mockito.any())).thenReturn(CompletableFuture.completedFuture(UpdateHandlingResult.success()));

        AtomicInteger beforeCalls = new AtomicInteger();
        AtomicInteger afterCalls = new AtomicInteger();
        UpdatePipelineHook hook = new UpdatePipelineHook() {
            @Override
            public void onBefore(Update update, UpdatePipelineContext context) {
                beforeCalls.incrementAndGet();
            }

            @Override
            public void onAfter(Update update, UpdatePipelineContext context, UpdatePipelineResult result) {
                afterCalls.incrementAndGet();
                assertNotNull(result);
            }
        };
        DefaultUpdatePipeline pipeline = new DefaultUpdatePipeline(sink, java.util.List.of(hook));

        UpdatePipelineResult result = pipeline.process(sampleUpdate("u-1"), UpdatePipelineContext.WEBHOOK)
                .toCompletableFuture()
                .join();

        assertTrue(result.isAccepted());
        assertEquals(1, beforeCalls.get());
        assertEquals(1, afterCalls.get());
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
