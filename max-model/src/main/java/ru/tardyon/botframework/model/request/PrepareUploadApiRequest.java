package ru.tardyon.botframework.model.request;

import java.util.Objects;

/**
 * Docs-shaped request for POST /uploads.
 */
public record PrepareUploadApiRequest(UploadType type) {
    public PrepareUploadApiRequest {
        Objects.requireNonNull(type, "type");
    }
}
