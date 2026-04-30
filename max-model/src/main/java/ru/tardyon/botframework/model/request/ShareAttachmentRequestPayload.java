package ru.tardyon.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request payload for attaching a shared message or attachment reference.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShareAttachmentRequestPayload(
        @JsonProperty("url") String url,
        @JsonProperty("token") String token
) {
    public ShareAttachmentRequestPayload {
        boolean hasUrl = url != null && !url.isBlank();
        boolean hasToken = token != null && !token.isBlank();
        if (!hasUrl && !hasToken) {
            throw new IllegalArgumentException("Either url or token must be provided");
        }
    }

    public static ShareAttachmentRequestPayload url(String value) {
        return new ShareAttachmentRequestPayload(value, null);
    }

    public static ShareAttachmentRequestPayload token(String value) {
        return new ShareAttachmentRequestPayload(null, value);
    }
}
