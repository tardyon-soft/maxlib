package ru.max.botframework.ingestion;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import ru.max.botframework.model.Update;

/**
 * Default single-threaded long polling runner.
 */
public final class DefaultLongPollingRunner implements LongPollingRunner {
    private final PollingUpdateSource source;
    private final UpdateSink sink;
    private final LongPollingRunnerConfig config;
    private final ExecutorService executor;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Future<?> worker;
    private volatile Long marker;

    public DefaultLongPollingRunner(PollingUpdateSource source, UpdateSink sink) {
        this(source, sink, LongPollingRunnerConfig.defaults());
    }

    public DefaultLongPollingRunner(PollingUpdateSource source, UpdateSink sink, LongPollingRunnerConfig config) {
        this.source = Objects.requireNonNull(source, "source");
        this.sink = Objects.requireNonNull(sink, "sink");
        this.config = Objects.requireNonNull(config, "config");
        this.executor = config.executor();
        this.marker = config.request().marker();
    }

    @Override
    public synchronized void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        worker = executor.submit(this::runLoop);
    }

    @Override
    public synchronized void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        Future<?> localWorker = worker;
        if (localWorker != null) {
            localWorker.cancel(true);
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void runLoop() {
        try {
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                PollingBatch batch = pollBatch();
                if (batch == null) {
                    sleep(config.sourceErrorDelay());
                    continue;
                }

                if (batch.nextMarker() != null) {
                    marker = batch.nextMarker();
                }

                if (batch.isEmpty()) {
                    sleep(config.idleDelay());
                    continue;
                }

                for (Update update : batch.updates()) {
                    if (!running.get() || Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    handleUpdate(update);
                }
            }
        } finally {
            running.set(false);
        }
    }

    private PollingBatch pollBatch() {
        PollingFetchRequest request = new PollingFetchRequest(
                marker,
                config.request().timeout(),
                config.request().limit(),
                config.request().types()
        );
        try {
            return source.poll(request);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void handleUpdate(Update update) {
        try {
            UpdateHandlingResult result = sink.handle(update).toCompletableFuture().join();
            if (result == null || !result.isSuccess()) {
                sleep(config.sinkErrorDelay());
            }
        } catch (CompletionException | RuntimeException ignored) {
            sleep(config.sinkErrorDelay());
        }
    }

    private void sleep(Duration delay) {
        long millis = delay.toMillis();
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            running.set(false);
        }
    }
}
