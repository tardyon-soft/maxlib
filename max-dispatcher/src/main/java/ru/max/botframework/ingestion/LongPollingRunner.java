package ru.max.botframework.ingestion;

/**
 * Lifecycle contract for long-polling execution loop.
 */
public interface LongPollingRunner extends AutoCloseable {

    void start();

    void stop();

    boolean isRunning();

    @Override
    default void close() {
        stop();
    }
}
