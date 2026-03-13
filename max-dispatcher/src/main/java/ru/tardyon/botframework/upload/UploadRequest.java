package ru.tardyon.botframework.upload;

import java.util.Objects;
import java.util.Optional;

/**
 * Upload orchestration request options.
 */
public record UploadRequest(
        UploadFlowType preferredFlowType,
        String mediaTypeHint
) {
    public UploadRequest {
        if (mediaTypeHint != null && mediaTypeHint.isBlank()) {
            throw new IllegalArgumentException("mediaTypeHint must not be blank");
        }
    }

    public static UploadRequest defaults() {
        return new UploadRequest(null, null);
    }

    public UploadRequest withPreferredFlow(UploadFlowType flowType) {
        return new UploadRequest(Objects.requireNonNull(flowType, "flowType"), mediaTypeHint);
    }

    public UploadRequest withMediaTypeHint(String hint) {
        return new UploadRequest(preferredFlowType, hint);
    }

    public Optional<UploadFlowType> preferredFlowTypeOptional() {
        return Optional.ofNullable(preferredFlowType);
    }

    public Optional<String> mediaTypeHintOptional() {
        return Optional.ofNullable(mediaTypeHint);
    }
}
