package ru.tardyon.botframework.ingestion;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tardyon.botframework.client.error.MaxRateLimitException;
import ru.tardyon.botframework.model.Update;

/**
 * Default single-threaded long polling runner.
 */
public final class DefaultLongPollingRunner implements LongPollingRunner {
    private static final Logger log = LoggerFactory.getLogger(DefaultLongPollingRunner.class);
    private final PollingUpdateSource source;
    private final UpdatePipeline pipeline;
    private final LongPollingRunnerConfig config;
    private final ExecutorService executor;
    private final PollingMarkerState markerState;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private volatile Future<?> worker;
    private volatile Duration nextSourceErrorDelay;

    public DefaultLongPollingRunner(PollingUpdateSource source, UpdateConsumer consumer) {
        this(source, wrap(consumer), LongPollingRunnerConfig.defaults());
    }

    public DefaultLongPollingRunner(PollingUpdateSource source, UpdateSink sink) {
        this(source, new DefaultUpdatePipeline(sink), LongPollingRunnerConfig.defaults());
    }

    public DefaultLongPollingRunner(PollingUpdateSource source, UpdateConsumer consumer, LongPollingRunnerConfig config) {
        this(source, new DefaultUpdatePipeline(wrap(consumer)), config, new InMemoryPollingMarkerState(config.request().marker()));
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
        if (shutdown.get()) {
            throw new IllegalStateException("Runner is already shutdown");
        }
        if (!running.compareAndSet(false, true)) {
            log.debug("Long polling start skipped: already running");
            return;
        }
        log.debug("Long polling started: request={}, shutdownTimeout={}", config.request(), config.shutdownTimeout());
        worker = executor.submit(this::runLoop);
    }

    @Override
    public synchronized void stop() {
        log.debug("Long polling stop requested");
        stopLoopAndAwait(config.shutdownTimeout());
    }

    @Override
    public synchronized void shutdown() {
        if (!shutdown.compareAndSet(false, true)) {
            log.debug("Long polling shutdown skipped: already shutdown");
            return;
        }
        log.debug("Long polling shutdown started");
        stopLoopAndAwait(config.shutdownTimeout());
        if (config.closeSourceOnShutdown()) {
            safeCloseSource();
        }
        if (config.closeExecutorOnShutdown()) {
            safeShutdownExecutor();
        }
    }

    private void stopLoopAndAwait(Duration timeout) {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        Future<?> localWorker = worker;
        if (localWorker != null) {
            awaitWorker(localWorker, timeout);
        }
        worker = null;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void runLoop() {
        log.debug("Long polling worker loop entered");
        try {
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                PollingBatch batch = pollBatch();
                if (batch == null) {
                    Duration delay = consumeNextSourceErrorDelay();
                    log.debug("Polling batch retrieval failed; delaying {}", delay);
                    sleep(delay);
                    continue;
                }

                if (batch.isEmpty()) {
                    log.debug("Polling batch empty; marker advanced to {}", batch.nextMarker());
                    markerState.advance(batch.nextMarker());
                    sleep(config.idleDelay());
                    continue;
                }
                log.debug("Polling batch received: size={}, marker={}", batch.updates().size(), batch.nextMarker());

                boolean batchHandledSuccessfully = true;
                for (Update update : batch.updates()) {
                    if (!running.get() || Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    if (!handleUpdate(update)) {
                        batchHandledSuccessfully = false;
                    }
                }

                markerState.advance(batch.nextMarker());
                if (batchHandledSuccessfully) {
                    log.debug("Polling batch handled successfully; marker advanced to {}", batch.nextMarker());
                } else {
                    log.debug("Polling batch handled with errors; marker still advanced to {}", batch.nextMarker());
                }
            }
        } finally {
            running.set(false);
            log.debug("Long polling worker loop exited");
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
        } catch (MaxRateLimitException rateLimitException) {
            Duration delay = rateLimitDelay(rateLimitException, config.sourceErrorDelay());
            nextSourceErrorDelay = delay;
            log.debug(
                    "Polling source rate limited for request: {}; retryAfterSeconds={}, delay={}",
                    request,
                    rateLimitException.retryAfterSeconds(),
                    delay,
                    rateLimitException
            );
            return null;
        } catch (RuntimeException ignored) {
            nextSourceErrorDelay = config.sourceErrorDelay();
            log.debug("Polling source error for request: {}", request, ignored);
            return null;
        }
    }

    private Duration consumeNextSourceErrorDelay() {
        Duration configured = config.sourceErrorDelay();
        Duration override = nextSourceErrorDelay;
        nextSourceErrorDelay = null;
        return override == null ? configured : override;
    }

    private static Duration rateLimitDelay(MaxRateLimitException exception, Duration fallback) {
        Long retryAfterSeconds = exception.retryAfterSeconds();
        if (retryAfterSeconds == null || retryAfterSeconds <= 0) {
            return fallback;
        }
        Duration retryAfter = Duration.ofSeconds(retryAfterSeconds);
        return retryAfter.compareTo(fallback) > 0 ? retryAfter : fallback;
    }

    private boolean handleUpdate(Update update) {
        try {
            UpdatePipelineResult result = pipeline.process(update, UpdatePipelineContext.POLLING).toCompletableFuture().join();
            if (result == null || !result.isAccepted()) {
                log.debug("Polling update rejected by pipeline: updateId={}", update.updateId().value());
                sleep(config.sinkErrorDelay());
                return false;
            }
            log.debug("Polling update accepted: updateId={}", update.updateId().value());
            return true;
        } catch (RuntimeException ignored) {
            log.debug("Polling update handling error: updateId={}", update.updateId().value(), ignored);
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

    private void awaitWorker(Future<?> localWorker, Duration timeout) {
        long timeoutMillis = timeout.toMillis();
        try {
            if (timeoutMillis <= 0) {
                localWorker.get();
            } else {
                localWorker.get(timeoutMillis, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            localWorker.cancel(true);
        } catch (TimeoutException timeoutException) {
            localWorker.cancel(true);
        } catch (ExecutionException ignored) {
            // worker errors are treated as transient ingestion failures
        }
    }

    private void safeCloseSource() {
        try {
            source.close();
        } catch (Exception ignored) {
            log.debug("Polling source close failed", ignored);
            // source cleanup errors must not break shutdown flow
        }
    }

    private void safeShutdownExecutor() {
        executor.shutdown();
        try {
            long timeoutMillis = config.shutdownTimeout().toMillis();
            if (timeoutMillis <= 0) {
                executor.awaitTermination(0, TimeUnit.MILLISECONDS);
            } else {
                executor.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
        if (!executor.isTerminated()) {
            executor.shutdownNow();
        }
        log.debug("Long polling executor shutdown completed: terminated={}", executor.isTerminated());
    }

    private static UpdateSink wrap(UpdateConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        return consumer::handle;
    }
}
