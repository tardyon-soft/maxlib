package ru.tardyon.botframework.ingestion;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of update processing in unified ingestion pipeline.
 */
public record UpdatePipelineResult(
        UpdatePipelineStatus status,
        Optional<Throwable> error
) {
    public UpdatePipelineResult {
        Objects.requireNonNull(status, "status");
    }

    public static UpdatePipelineResult accepted() {
        return new UpdatePipelineResult(UpdatePipelineStatus.ACCEPTED, Optional.empty());
    }

    public static UpdatePipelineResult rejected(Throwable error) {
        return new UpdatePipelineResult(UpdatePipelineStatus.REJECTED, Optional.ofNullable(error));
    }

    public boolean isAccepted() {
        return status == UpdatePipelineStatus.ACCEPTED;
    }

    public Optional<Throwable> error() {
        return error;
    }
}
