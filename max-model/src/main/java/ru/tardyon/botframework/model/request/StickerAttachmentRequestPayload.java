package ru.tardyon.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Request payload for attaching a sticker.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StickerAttachmentRequestPayload(
        @JsonProperty("code") String code
) {
    public StickerAttachmentRequestPayload {
        Objects.requireNonNull(code, "code");
        if (code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
    }
}
