package ru.tardyon.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ru.tardyon.botframework.model.FileId;

/**
 * Minimal attachment reference abstraction for outgoing media requests.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AttachmentInput(
        FileId fileId,
        String uploadRef,
        String url
) {
    public AttachmentInput {
        boolean hasValue = fileId != null || notBlank(uploadRef) || notBlank(url);
        if (!hasValue) {
            throw new IllegalArgumentException("At least one attachment input reference must be provided");
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
