package ru.max.botframework.upload;

import java.util.Objects;
import java.util.Optional;

/**
 * Final upload result used by media mapping layer.
 */
public record UploadResult(
        UploadRef ref,
        UploadFlowType flowType,
        long bytesTransferred,
        String contentType
) {
    public UploadResult {
        Objects.requireNonNull(ref, "ref");
        Objects.requireNonNull(flowType, "flowType");
        if (bytesTransferred < 0) {
            throw new IllegalArgumentException("bytesTransferred must be non-negative");
        }
        if (contentType != null && contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
    }

    public Optional<String> contentTypeOptional() {
        return Optional.ofNullable(contentType);
    }
}
