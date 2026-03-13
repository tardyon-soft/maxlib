package ru.max.botframework.upload;

import java.net.URI;
import java.util.Objects;

/**
 * Result of upload preparation step.
 */
public record UploadPreparation(
        URI uploadUrl,
        UploadFlowType flowType,
        String uploadId
) {
    public UploadPreparation {
        Objects.requireNonNull(uploadUrl, "uploadUrl");
        Objects.requireNonNull(flowType, "flowType");
        Objects.requireNonNull(uploadId, "uploadId");
        if (uploadId.isBlank()) {
            throw new IllegalArgumentException("uploadId must not be blank");
        }
    }
}
