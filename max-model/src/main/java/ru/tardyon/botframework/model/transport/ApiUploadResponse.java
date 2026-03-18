package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MAX API transport response for POST /uploads.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiUploadResponse(
        @JsonProperty("url") String url,
        @JsonProperty("token") String token
) {
}
