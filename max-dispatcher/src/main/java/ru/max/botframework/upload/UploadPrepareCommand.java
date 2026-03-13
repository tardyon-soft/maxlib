package ru.max.botframework.upload;

import java.util.Objects;

/**
 * Upload preparation command for {@code POST /uploads} step.
 */
public record UploadPrepareCommand(
        String fileName,
        String contentType,
        Long size,
        UploadFlowType preferredFlowType,
        String mediaTypeHint
) {
    public UploadPrepareCommand {
        Objects.requireNonNull(fileName, "fileName");
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (contentType != null && contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
        if (size != null && size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        if (mediaTypeHint != null && mediaTypeHint.isBlank()) {
            throw new IllegalArgumentException("mediaTypeHint must not be blank");
        }
    }
}
