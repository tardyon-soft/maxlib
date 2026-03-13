package ru.max.botframework.upload;

import java.net.URI;
import java.util.Objects;

/**
 * Low-level request model for single resumable chunk transfer.
 */
public record ResumableChunkUploadRequest(
        URI uploadUrl,
        String uploadId,
        String fileName,
        String contentType,
        long chunkOffset,
        Long totalBytes,
        boolean lastChunk,
        int attempt,
        byte[] chunk
) {
    public ResumableChunkUploadRequest {
        Objects.requireNonNull(uploadUrl, "uploadUrl");
        Objects.requireNonNull(uploadId, "uploadId");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(chunk, "chunk");
        if (uploadId.isBlank()) {
            throw new IllegalArgumentException("uploadId must not be blank");
        }
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
        if (chunkOffset < 0) {
            throw new IllegalArgumentException("chunkOffset must be non-negative");
        }
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be >= 1");
        }
        if (totalBytes != null && totalBytes < 0) {
            throw new IllegalArgumentException("totalBytes must be non-negative");
        }
        if (chunk.length == 0) {
            throw new IllegalArgumentException("chunk must not be empty");
        }
        chunk = chunk.clone();
    }

    @Override
    public byte[] chunk() {
        return chunk.clone();
    }
}
