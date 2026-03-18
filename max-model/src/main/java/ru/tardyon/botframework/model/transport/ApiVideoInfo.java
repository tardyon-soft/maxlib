package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * MAX API transport response for GET /videos/{videoToken}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiVideoInfo(
        @JsonProperty("token") String token,
        @JsonProperty("urls") Map<String, String> urls,
        @JsonProperty("thumbnail") Object thumbnail,
        @JsonProperty("width") Integer width,
        @JsonProperty("height") Integer height,
        @JsonProperty("duration") Integer duration
) {
    public ApiVideoInfo {
        urls = urls == null ? Map.of() : Map.copyOf(urls);
    }
}
