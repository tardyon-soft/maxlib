package ru.max.botframework.ingestion;

import java.util.concurrent.CompletionStage;
import ru.max.botframework.model.Update;

/**
 * Unified transport-level ingestion pipeline for all update sources.
 */
public interface UpdatePipeline {

    CompletionStage<UpdatePipelineResult> process(Update update, UpdatePipelineContext context);
}
