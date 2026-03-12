package ru.max.botframework.ingestion;

/**
 * Common lifecycle boundary for transport-specific update sources.
 */
public interface UpdateSource extends AutoCloseable {

    @Override
    default void close() {
        // no-op by default
    }
}
