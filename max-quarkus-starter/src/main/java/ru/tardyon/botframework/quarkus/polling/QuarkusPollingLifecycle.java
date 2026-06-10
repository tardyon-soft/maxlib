package ru.tardyon.botframework.quarkus.polling;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Quarkus lifecycle bridge that starts/stops {@link ru.tardyon.botframework.ingestion.LongPollingRunner}
 * with application context lifecycle.
 */
@Singleton
@Startup
public class QuarkusPollingLifecycle {
    private final QuarkusPollingBootstrap bootstrap;

    @Inject
    public QuarkusPollingLifecycle(QuarkusPollingBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @PostConstruct
    void start() {
        bootstrap.start();
    }

    @PreDestroy
    void stop() {
        bootstrap.stop();
        bootstrap.shutdown();
    }

    public boolean isRunning() {
        return bootstrap.isRunning();
    }
}
