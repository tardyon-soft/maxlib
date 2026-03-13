package ru.max.botframework.spring.polling;

import java.util.Objects;
import org.springframework.context.SmartLifecycle;
import ru.max.botframework.ingestion.LongPollingRunner;

/**
 * Spring lifecycle bridge that starts/stops {@link LongPollingRunner} with application context lifecycle.
 */
public final class SpringPollingLifecycle implements SmartLifecycle {
    private final LongPollingRunner runner;
    private volatile boolean running;

    public SpringPollingLifecycle(LongPollingRunner runner) {
        this.runner = Objects.requireNonNull(runner, "runner");
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        runner.start();
        running = true;
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        runner.stop();
        running = false;
    }

    @Override
    public void stop(Runnable callback) {
        try {
            stop();
        } finally {
            callback.run();
        }
    }

    @Override
    public boolean isRunning() {
        return running && runner.isRunning();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
