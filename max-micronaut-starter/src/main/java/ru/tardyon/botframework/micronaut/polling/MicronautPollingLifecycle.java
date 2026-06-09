package ru.tardyon.botframework.micronaut.polling;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Objects;
import ru.tardyon.botframework.ingestion.LongPollingRunner;

/**
 * Micronaut lifecycle bridge that starts/stops {@link LongPollingRunner} with application context lifecycle.
 */
@Context
@Requires(beans = LongPollingRunner.class)
public final class MicronautPollingLifecycle {
    private final LongPollingRunner runner;
    private volatile boolean running;

    public MicronautPollingLifecycle(LongPollingRunner runner) {
        this.runner = Objects.requireNonNull(runner, "runner");
    }

    @PostConstruct
    synchronized void start() {
        if (running) {
            return;
        }
        runner.start();
        running = true;
    }

    @PreDestroy
    synchronized void stop() {
        if (!running) {
            return;
        }
        runner.stop();
        running = false;
    }

    public boolean isRunning() {
        return running && runner.isRunning();
    }
}
