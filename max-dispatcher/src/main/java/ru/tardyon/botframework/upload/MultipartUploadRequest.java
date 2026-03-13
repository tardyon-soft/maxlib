package ru.tardyon.botframework.upload;

import java.net.URI;
import java.util.Objects;

/**
 * Low-level multipart upload request model.
 */
public record MultipartUploadRequest(
        URI uploadUrl,
        String fileName,
        String contentType,
        byte[] content
) {
    public MultipartUploadRequest {
        Objects.requireNonNull(uploadUrl, "uploadUrl");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(content, "content");
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
