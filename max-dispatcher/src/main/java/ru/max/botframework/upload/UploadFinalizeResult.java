package ru.max.botframework.upload;

import java.util.Objects;

/**
 * Finalization response payload returned by upload API.
 */
public record UploadFinalizeResult(
        String uploadRef,
        Long size,
        String contentType
) {
    public UploadFinalizeResult {
        Objects.requireNonNull(uploadRef, "uploadRef");
        if (uploadRef.isBlank()) {
            throw new IllegalArgumentException("uploadRef must not be blank");
        }
        if (size != null && size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        if (contentType != null && contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
    }
}
