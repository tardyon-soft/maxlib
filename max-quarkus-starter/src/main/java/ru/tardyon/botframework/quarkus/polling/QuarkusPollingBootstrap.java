package ru.tardyon.botframework.quarkus.polling;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import ru.tardyon.botframework.ingestion.LongPollingRunner;

/**
 * Thin Quarkus-facing wrapper around {@link LongPollingRunner}.
 */
@Singleton
public class QuarkusPollingBootstrap {
    private final Instance<LongPollingRunner> longPollingRunnerProvider;

    @Inject
    public QuarkusPollingBootstrap(Instance<LongPollingRunner> longPollingRunnerProvider) {
        this.longPollingRunnerProvider = longPollingRunnerProvider;
    }

    public Optional<LongPollingRunner> longPollingRunner() {
        if (longPollingRunnerProvider == null || !longPollingRunnerProvider.isResolvable()) {
            return Optional.empty();
        }
        return Optional.ofNullable(longPollingRunnerProvider.get());
    }

    public void start() {
        longPollingRunner().ifPresent(LongPollingRunner::start);
    }

    public void stop() {
        longPollingRunner().ifPresent(LongPollingRunner::stop);
    }

    public void shutdown() {
        longPollingRunner().ifPresent(LongPollingRunner::shutdown);
    }

    public boolean isRunning() {
        return longPollingRunner().map(LongPollingRunner::isRunning).orElse(false);
    }
}
