package ru.max.botframework.upload;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Final upload result used by media mapping layer.
 */
public record UploadResult(
        UploadRef ref,
        UploadFlowType flowType,
        long bytesTransferred,
        String contentType,
        UploadMediaKind mediaKind,
        Map<String, String> attachmentPayload
) {
    public UploadResult(
            UploadRef ref,
            UploadFlowType flowType,
            long bytesTransferred,
            String contentType
    ) {
        this(ref, flowType, bytesTransferred, contentType, UploadMediaKind.UNKNOWN, Map.of());
    }

    public UploadResult {
        Objects.requireNonNull(ref, "ref");
        Objects.requireNonNull(flowType, "flowType");
        Objects.requireNonNull(mediaKind, "mediaKind");
        Objects.requireNonNull(attachmentPayload, "attachmentPayload");
        if (bytesTransferred < 0) {
            throw new IllegalArgumentException("bytesTransferred must be non-negative");
        }
        if (contentType != null && contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
        attachmentPayload = Map.copyOf(attachmentPayload);
    }

    public Optional<String> contentTypeOptional() {
        return Optional.ofNullable(contentType);
    }

    public Optional<String> attachmentPayloadValue(String key) {
        Objects.requireNonNull(key, "key");
        return Optional.ofNullable(attachmentPayload.get(key));
    }
}
