package ru.tardyon.botframework.model.transport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MAX API transport update shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiUpdate(
        @JsonProperty("update_type") String updateType,
        @JsonProperty("timestamp") Long timestamp,
        @JsonProperty("message") ApiMessage message,
        @JsonProperty("callback") ApiCallback callback,
        @JsonProperty("user_locale") String userLocale
) {
}
