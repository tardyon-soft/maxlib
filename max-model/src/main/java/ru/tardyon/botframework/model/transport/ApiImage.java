package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MAX API generic image object.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiImage(
        @JsonProperty("url") String url
) {
}
