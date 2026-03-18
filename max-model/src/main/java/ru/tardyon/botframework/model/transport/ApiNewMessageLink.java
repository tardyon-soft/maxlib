package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MAX API transport new-message link shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiNewMessageLink(
        @JsonProperty("type") String type,
        @JsonProperty("message") String message
) {
}
