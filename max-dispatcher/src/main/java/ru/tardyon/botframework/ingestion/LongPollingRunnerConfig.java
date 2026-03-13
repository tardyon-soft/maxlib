package ru.tardyon.botframework.ingestion;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Runtime settings for {@link DefaultLongPollingRunner}.
 */
public record LongPollingRunnerConfig(
        PollingFetchRequest request,
        Duration idleDelay,
        Duration sourceErrorDelay,
        Duration sinkErrorDelay,
        ExecutorService executor,
        Duration shutdownTimeout,
        boolean closeSourceOnShutdown,
        boolean closeExecutorOnShutdown
) {
    private static final Duration DEFAULT_IDLE_DELAY = Duration.ofMillis(100);
    private static final Duration DEFAULT_SOURCE_ERROR_DELAY = Duration.ofSeconds(1);
    private static final Duration DEFAULT_SINK_ERROR_DELAY = Duration.ofMillis(200);
    private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);

    public LongPollingRunnerConfig {
        Objects.requireNonNull(request, "request");
        idleDelay = requireNonNegative(idleDelay, "idleDelay");
        sourceErrorDelay = requireNonNegative(sourceErrorDelay, "sourceErrorDelay");
        sinkErrorDelay = requireNonNegative(sinkErrorDelay, "sinkErrorDelay");
        Objects.requireNonNull(executor, "executor");
        shutdownTimeout = requireNonNegative(shutdownTimeout, "shutdownTimeout");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static LongPollingRunnerConfig defaults() {
        return builder().build();
    }

    public static final class Builder {
        private PollingFetchRequest request = PollingFetchRequest.defaults();
        private Duration idleDelay = DEFAULT_IDLE_DELAY;
        private Duration sourceErrorDelay = DEFAULT_SOURCE_ERROR_DELAY;
        private Duration sinkErrorDelay = DEFAULT_SINK_ERROR_DELAY;
        private ExecutorService executor = singleThreadExecutor();
        private Duration shutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;
        private boolean closeSourceOnShutdown = true;
        private boolean closeExecutorOnShutdown = true;

        public Builder request(PollingFetchRequest request) {
            this.request = Objects.requireNonNull(request, "request");
            return this;
        }

        public Builder idleDelay(Duration idleDelay) {
            this.idleDelay = requireNonNegative(idleDelay, "idleDelay");
            return this;
        }

        public Builder sourceErrorDelay(Duration sourceErrorDelay) {
            this.sourceErrorDelay = requireNonNegative(sourceErrorDelay, "sourceErrorDelay");
            return this;
        }

        public Builder sinkErrorDelay(Duration sinkErrorDelay) {
            this.sinkErrorDelay = requireNonNegative(sinkErrorDelay, "sinkErrorDelay");
            return this;
        }

        public Builder executor(ExecutorService executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
            this.closeExecutorOnShutdown = false;
            return this;
        }

        public Builder executor(ExecutorService executor, boolean closeOnShutdown) {
            this.executor = Objects.requireNonNull(executor, "executor");
            this.closeExecutorOnShutdown = closeOnShutdown;
            return this;
        }

        public Builder shutdownTimeout(Duration shutdownTimeout) {
            this.shutdownTimeout = requireNonNegative(shutdownTimeout, "shutdownTimeout");
            return this;
        }

        public Builder closeSourceOnShutdown(boolean closeSourceOnShutdown) {
            this.closeSourceOnShutdown = closeSourceOnShutdown;
            return this;
        }

        public Builder closeExecutorOnShutdown(boolean closeExecutorOnShutdown) {
            this.closeExecutorOnShutdown = closeExecutorOnShutdown;
            return this;
        }

        public LongPollingRunnerConfig build() {
            return new LongPollingRunnerConfig(
                    request,
                    idleDelay,
                    sourceErrorDelay,
                    sinkErrorDelay,
                    executor,
                    shutdownTimeout,
                    closeSourceOnShutdown,
                    closeExecutorOnShutdown
            );
        }
    }

    private static Duration requireNonNegative(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isNegative()) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }

    private static ExecutorService singleThreadExecutor() {
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "max-long-polling-runner");
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newSingleThreadExecutor(threadFactory);
    }
}
