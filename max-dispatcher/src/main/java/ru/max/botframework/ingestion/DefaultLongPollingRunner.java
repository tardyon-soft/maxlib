package ru.max.botframework.ingestion;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import ru.max.botframework.model.Update;

/**
 * Default single-threaded long polling runner.
 */
public final class DefaultLongPollingRunner implements LongPollingRunner {
    private final PollingUpdateSource source;
    private final UpdatePipeline pipeline;
    private final LongPollingRunnerConfig config;
    private final ExecutorService executor;
    private final PollingMarkerState markerState;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Future<?> worker;
    public DefaultLongPollingRunner(PollingUpdateSource source, UpdateSink sink) {
        this(source, new DefaultUpdatePipeline(sink), LongPollingRunnerConfig.defaults());
    }

    public DefaultLongPollingRunner(PollingUpdateSource source, UpdateSink sink, LongPollingRunnerConfig config) {
        this(source, new DefaultUpdatePipeline(sink), config, new InMemoryPollingMarkerState(config.request().marker()));
    }

    public DefaultLongPollingRunner(PollingUpdateSource source, UpdatePipeline pipeline) {
        this(source, pipeline, LongPollingRunnerConfig.defaults());
    }

    public DefaultLongPollingRunner(PollingUpdateSource source, UpdatePipeline pipeline, LongPollingRunnerConfig config) {
        this(source, pipeline, config, new InMemoryPollingMarkerState(config.request().marker()));
    }

    public DefaultLongPollingRunner(
            PollingUpdateSource source,
            UpdatePipeline pipeline,
            LongPollingRunnerConfig config,
            PollingMarkerState markerState
    ) {
        this.source = Objects.requireNonNull(source, "source");
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
        this.config = Objects.requireNonNull(config, "config");
        this.executor = config.executor();
        this.markerState = Objects.requireNonNull(markerState, "markerState");
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

                if (batch.isEmpty()) {
                    markerState.advance(batch.nextMarker());
                    sleep(config.idleDelay());
                    continue;
                }

                boolean batchHandledSuccessfully = true;
                for (Update update : batch.updates()) {
                    if (!running.get() || Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    if (!handleUpdate(update)) {
                        batchHandledSuccessfully = false;
                    }
                }

                if (batchHandledSuccessfully) {
                    markerState.advance(batch.nextMarker());
                }
            }
        } finally {
            running.set(false);
        }
    }

    private PollingBatch pollBatch() {
        PollingFetchRequest request = new PollingFetchRequest(
                markerState.current(),
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

    private boolean handleUpdate(Update update) {
        try {
            UpdatePipelineResult result = pipeline.process(update, UpdatePipelineContext.POLLING).toCompletableFuture().join();
            if (result == null || !result.isAccepted()) {
                sleep(config.sinkErrorDelay());
                return false;
            }
            return true;
        } catch (RuntimeException ignored) {
            sleep(config.sinkErrorDelay());
            return false;
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
