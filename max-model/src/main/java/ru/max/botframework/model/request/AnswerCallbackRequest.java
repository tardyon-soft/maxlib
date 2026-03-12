package ru.max.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;
import ru.max.botframework.model.CallbackId;

/**
 * Request DTO for answering callback events.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnswerCallbackRequest(
        CallbackId callbackId,
        String text,
        @JsonProperty("notify") Boolean sendNotification,
        Integer cacheSeconds
) {
    public AnswerCallbackRequest {
        Objects.requireNonNull(callbackId, "callbackId");
        sendNotification = sendNotification == null ? Boolean.TRUE : sendNotification;
        cacheSeconds = cacheSeconds == null ? 0 : cacheSeconds;

        if (cacheSeconds < 0) {
            throw new IllegalArgumentException("cacheSeconds must be non-negative");
        }
    }
}
