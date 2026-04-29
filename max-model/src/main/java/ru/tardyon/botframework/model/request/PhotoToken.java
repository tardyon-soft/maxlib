package ru.tardyon.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Uploaded photo token entry returned by MAX upload flow.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PhotoToken(
        @JsonProperty("token") String token
) {
}
