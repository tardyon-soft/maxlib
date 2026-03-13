package ru.max.botframework.upload;

/**
 * Low-level response model for single resumable chunk transfer.
 */
public record ResumableChunkUploadResponse(int statusCode, Long committedBytes) {
    public ResumableChunkUploadResponse {
        if (committedBytes != null && committedBytes < 0) {
            throw new IllegalArgumentException("committedBytes must be non-negative");
        }
    }
}
