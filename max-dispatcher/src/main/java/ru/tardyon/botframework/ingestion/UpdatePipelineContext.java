package ru.tardyon.botframework.ingestion;

import java.util.Objects;

/**
 * Minimal metadata passed through unified ingestion pipeline.
 */
public record UpdatePipelineContext(UpdateSourceType sourceType) {
    public static final UpdatePipelineContext POLLING = new UpdatePipelineContext(UpdateSourceType.POLLING);
    public static final UpdatePipelineContext WEBHOOK = new UpdatePipelineContext(UpdateSourceType.WEBHOOK);

    public UpdatePipelineContext {
        Objects.requireNonNull(sourceType, "sourceType");
    }
}
