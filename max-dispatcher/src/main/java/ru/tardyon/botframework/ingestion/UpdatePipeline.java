package ru.tardyon.botframework.ingestion;

import java.util.concurrent.CompletionStage;
import ru.tardyon.botframework.model.Update;

/**
 * Unified transport-level ingestion pipeline for all update sources.
 */
public interface UpdatePipeline {

    /**
     * Processes a normalized update from the given transport context.
     */
    CompletionStage<UpdatePipelineResult> process(Update update, UpdatePipelineContext context);
}
