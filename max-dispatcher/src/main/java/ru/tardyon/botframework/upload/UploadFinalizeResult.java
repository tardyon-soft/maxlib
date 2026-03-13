package ru.tardyon.botframework.upload;

import java.util.Map;
import java.util.Objects;

/**
 * Finalization response payload returned by upload API.
 */
public record UploadFinalizeResult(
        String uploadRef,
        Long size,
        String contentType,
        UploadMediaKind mediaKind,
        Map<String, String> attachmentPayload
) {
    public UploadFinalizeResult(String uploadRef, Long size, String contentType) {
        this(uploadRef, size, contentType, UploadMediaKind.UNKNOWN, Map.of());
    }

    public UploadFinalizeResult {
        Objects.requireNonNull(uploadRef, "uploadRef");
        Objects.requireNonNull(mediaKind, "mediaKind");
        Objects.requireNonNull(attachmentPayload, "attachmentPayload");
        if (uploadRef.isBlank()) {
            throw new IllegalArgumentException("uploadRef must not be blank");
        }
        if (size != null && size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        if (contentType != null && contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
        attachmentPayload = Map.copyOf(attachmentPayload);
    }
}
