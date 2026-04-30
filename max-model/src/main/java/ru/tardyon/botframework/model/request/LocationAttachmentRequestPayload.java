package ru.tardyon.botframework.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request payload for attaching geo location.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocationAttachmentRequestPayload(
        @JsonProperty("lat") double latitude,
        @JsonProperty("lon") double longitude
) {
}
