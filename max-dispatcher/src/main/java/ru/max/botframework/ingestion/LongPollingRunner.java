package ru.max.botframework.ingestion;

/**
 * Lifecycle contract for long-polling execution loop.
 */
public interface LongPollingRunner extends AutoCloseable {

    /**
     * Starts polling loop execution.
     */
    void start();

    /**
     * Gracefully stops polling loop without final resource disposal.
     */
    void stop();

    /**
     * Final shutdown including optional owned resource cleanup.
     */
    void shutdown();

    boolean isRunning();

    @Override
    default void close() {
        shutdown();
    }
}
