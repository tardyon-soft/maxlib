package ru.max.botframework.spring.polling;

import java.util.Objects;
import java.util.Optional;
import ru.max.botframework.ingestion.LongPollingRunner;

/**
 * Thin Spring-facing wrapper around {@link LongPollingRunner}.
 */
public final class SpringPollingBootstrap {
    private final LongPollingRunner longPollingRunner;

    public SpringPollingBootstrap(LongPollingRunner longPollingRunner) {
        this.longPollingRunner = longPollingRunner;
    }

    public Optional<LongPollingRunner> longPollingRunner() {
        return Optional.ofNullable(longPollingRunner);
    }

    public void start() {
        requireRunner().start();
    }

    public void stop() {
        requireRunner().stop();
    }

    public void shutdown() {
        requireRunner().shutdown();
    }

    public boolean isRunning() {
        return longPollingRunner != null && longPollingRunner.isRunning();
    }

    private LongPollingRunner requireRunner() {
        return Objects.requireNonNull(
                longPollingRunner,
                "LongPollingRunner is not configured. Provide LongPollingRunner bean for polling mode."
        );
    }
}
