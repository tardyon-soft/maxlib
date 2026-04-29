package ru.tardyon.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Request payload for attaching or setting a chat image.
 *
 * <p>MAX API requires these fields to be mutually exclusive.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PhotoAttachmentRequestPayload(
        @JsonProperty("url") String url,
        @JsonProperty("token") String token,
        @JsonProperty("photos") Map<String, PhotoToken> photos
) {
    public PhotoAttachmentRequestPayload {
        photos = photos == null ? null : Map.copyOf(photos);
        int filled = filled(url) + filled(token) + filled(photos);
        if (filled != 1) {
            throw new IllegalArgumentException("Exactly one photo reference field must be provided");
        }
    }

    public static PhotoAttachmentRequestPayload url(String value) {
        return new PhotoAttachmentRequestPayload(value, null, null);
    }

    public static PhotoAttachmentRequestPayload token(String value) {
        return new PhotoAttachmentRequestPayload(null, value, null);
    }

    public static PhotoAttachmentRequestPayload photos(Map<String, PhotoToken> value) {
        return new PhotoAttachmentRequestPayload(null, null, value);
    }

    private static int filled(String value) {
        return value == null || value.isBlank() ? 0 : 1;
    }

    private static int filled(Map<?, ?> value) {
        return value == null || value.isEmpty() ? 0 : 1;
    }
}
