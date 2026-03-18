package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Docs-shaped outgoing attachment request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiAttachmentRequest(
        @JsonProperty("type") String type,
        @JsonProperty("payload") Object payload
) {
}
