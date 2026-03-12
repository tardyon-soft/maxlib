package ru.max.botframework.ingestion;

/**
 * Transport-agnostic update source lifecycle.
 */
public interface UpdateSource extends AutoCloseable {

    void start(UpdateSink sink);

    void stop();

    @Override
    default void close() {
        stop();
    }
}
