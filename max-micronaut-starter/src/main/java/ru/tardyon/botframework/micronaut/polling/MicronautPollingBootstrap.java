package ru.tardyon.botframework.micronaut.polling;

import java.util.Optional;
import ru.tardyon.botframework.ingestion.LongPollingRunner;

/**
 * Thin Micronaut-facing wrapper around {@link LongPollingRunner}.
 */
public final class MicronautPollingBootstrap {
    private final LongPollingRunner longPollingRunner;

    public MicronautPollingBootstrap(LongPollingRunner longPollingRunner) {
        this.longPollingRunner = longPollingRunner;
    }

    public Optional<LongPollingRunner> longPollingRunner() {
        return Optional.ofNullable(longPollingRunner);
    }

    public void start() {
        if (longPollingRunner == null) {
            return;
        }
        longPollingRunner.start();
    }

    public void stop() {
        if (longPollingRunner == null) {
            return;
        }
        longPollingRunner.stop();
    }

    public void shutdown() {
        if (longPollingRunner == null) {
            return;
        }
        longPollingRunner.shutdown();
    }

    public boolean isRunning() {
        return longPollingRunner != null && longPollingRunner.isRunning();
    }
}
