package ru.max.botframework.ingestion;

import ru.max.botframework.model.Update;

/**
 * Extension point for logging/metrics around ingestion pipeline.
 */
public interface UpdatePipelineHook {

    default void onBefore(Update update, UpdatePipelineContext context) {
    }

    default void onAfter(Update update, UpdatePipelineContext context, UpdatePipelineResult result) {
    }
}
