package ru.max.botframework.ingestion;

import ru.max.botframework.model.Update;

/**
 * Unified consumer contract for normalized updates.
 */
@FunctionalInterface
public interface UpdateSink {

    void accept(Update update);
}
