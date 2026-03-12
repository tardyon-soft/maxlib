package ru.max.botframework.ingestion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.max.botframework.client.error.MaxApiException;
import ru.max.botframework.model.Update;
import ru.max.botframework.model.UpdateId;
import ru.max.botframework.model.UpdateType;

class DefaultLongPollingRunnerTest {

    @Test
    void startProcessesUpdatesAndSendsThemToSink() throws Exception {
        PollingUpdateSource source = Mockito.mock(PollingUpdateSource.class);
        UpdateSink sink = Mockito.mock(UpdateSink.class);
        CountDownLatch updatesHandled = new CountDownLatch(2);
        AtomicInteger pollCalls = new AtomicInteger();

        when(source.poll(any())).thenAnswer(invocation -> {
            if (pollCalls.incrementAndGet() == 1) {
                return new PollingBatch(List.of(sampleUpdate("u-1"), sampleUpdate("u-2")), 10L);
            }
            return new PollingBatch(List.of(), 10L);
        });
        when(sink.handle(any())).thenAnswer(invocation -> {
            updatesHandled.countDown();
            return CompletableFuture.completedFuture(UpdateHandlingResult.success());
        });

        DefaultLongPollingRunner runner = runner(source, sink);
        runner.start();

        assertTrue(updatesHandled.await(1, TimeUnit.SECONDS));
        runner.stop();
        awaitCondition(() -> !runner.isRunning(), Duration.ofSeconds(1));

        verify(sink, atLeast(2)).handle(any());
        verify(source, atLeast(1)).poll(any());
    }

    @Test
    void stopStopsRunnerLoop() throws Exception {
        PollingUpdateSource source = Mockito.mock(PollingUpdateSource.class);
        UpdateSink sink = Mockito.mock(UpdateSink.class);
        CountDownLatch pollCalled = new CountDownLatch(1);

        when(source.poll(any())).thenAnswer(invocation -> {
            pollCalled.countDown();
            return new PollingBatch(List.of(), 100L);
        });

        DefaultLongPollingRunner runner = runner(source, sink);
        runner.start();
        assertTrue(pollCalled.await(1, TimeUnit.SECONDS));

        runner.stop();
        awaitCondition(() -> !runner.isRunning(), Duration.ofSeconds(1));

        assertFalse(runner.isRunning());
    }

    @Test
    void pollingSourceErrorIsTreatedAsTransientAndLoopContinues() throws Exception {
        PollingUpdateSource source = Mockito.mock(PollingUpdateSource.class);
        UpdateSink sink = Mockito.mock(UpdateSink.class);
        CountDownLatch secondPollReached = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();

        when(source.poll(any())).thenAnswer(invocation -> {
            if (calls.incrementAndGet() == 1) {
                throw new MaxApiException(503, "temporary");
            }
            secondPollReached.countDown();
            return new PollingBatch(List.of(), 200L);
        });

        DefaultLongPollingRunner runner = runner(source, sink);
        runner.start();

        assertTrue(secondPollReached.await(1, TimeUnit.SECONDS));
        runner.stop();
        awaitCondition(() -> !runner.isRunning(), Duration.ofSeconds(1));

        verify(source, atLeast(2)).poll(any());
        verify(sink, never()).handle(any());
    }

    @Test
    void sinkFailureDoesNotStopPollingLoop() throws Exception {
        PollingUpdateSource source = Mockito.mock(PollingUpdateSource.class);
        UpdateSink sink = Mockito.mock(UpdateSink.class);
        CountDownLatch secondPollReached = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();

        when(source.poll(any())).thenAnswer(invocation -> {
            if (calls.incrementAndGet() == 1) {
                return new PollingBatch(List.of(sampleUpdate("u-1")), 300L);
            }
            secondPollReached.countDown();
            return new PollingBatch(List.of(), 300L);
        });
        when(sink.handle(any())).thenReturn(
                CompletableFuture.completedFuture(UpdateHandlingResult.failure(new RuntimeException("sink failed")))
        );

        DefaultLongPollingRunner runner = runner(source, sink);
        runner.start();

        assertTrue(secondPollReached.await(1, TimeUnit.SECONDS));
        runner.stop();
        awaitCondition(() -> !runner.isRunning(), Duration.ofSeconds(1));

        verify(source, atLeast(2)).poll(any());
        verify(sink, atLeast(1)).handle(any());
    }

    private static DefaultLongPollingRunner runner(PollingUpdateSource source, UpdateSink sink) {
        LongPollingRunnerConfig config = LongPollingRunnerConfig.builder()
                .idleDelay(Duration.ofMillis(10))
                .sourceErrorDelay(Duration.ofMillis(10))
                .sinkErrorDelay(Duration.ofMillis(10))
                .build();
        return new DefaultLongPollingRunner(source, sink, config);
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

    private static void awaitCondition(BooleanSupplier condition, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(10L);
        }
        throw new AssertionError("Condition was not met within timeout");
    }
}
